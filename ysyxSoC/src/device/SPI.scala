package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

import scala.collection.mutable.{ArrayBuffer, HashSet}

class SPIIO(val ssWidth: Int = 8) extends Bundle {
  val sck = Output(Bool())
  val ss = Output(UInt(ssWidth.W))
  val mosi = Output(Bool())
  val miso = Input(Bool())
}

class spi_top_apb extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
    val spi = new SPIIO
    val spi_irq_out = Output(Bool())
  })
}

class flash extends BlackBox {
  val io = IO(Flipped(new SPIIO(1)))
}

// 定义一个标准的硬件函数模板
abstract class HwFunction[T <: Data, R <: Data](argType: T, retType: R) {
  // 这就是 a0 (参数)
  val args = Wire(argType) 
  // 这就是 v0 (返回值)
  val ret  = Wire(retType)
  
  // 控制信号
  val enable = Wire(Bool()) // Caller 发出的 start
  val done   = Wire(Bool()) // Callee 返回的 return

  // 默认不启动，防止多驱动冲突
  args   := DontCare
  enable := false.B

  // 实现具体的硬件逻辑，返回 done 信号
  def impl(): Unit  
}

import chisel3._
import chisel3.util._
import scala.collection.mutable.ArrayBuffer

class HardwareThread(val name: String) {

  private val steps = ArrayBuffer[() => Unit]()
  private val globals = ArrayBuffer[() => Unit]()
  

  private var pcEntity: UInt = _
  private var isCombinational = false



  private val active = RegInit(false.B)

    // --- 控制信号 (Lazy Binding) ---
  private var startSignal: Bool = false.B  // 触发启动
  private var abortSignal: Bool = false.B  // 强制复位 (Kill)
  private var pauseSignal: Bool = false.B  // 暂停执行 (Stall)


  private val ownedSignals = HashSet[Data]()


  def pc: UInt = {
    require(pcEntity != null, "Error: Cannot access 'thread.pc' outside of Step/Call/Global logic! Hardware not generated yet.")
    pcEntity
  }


  // 设置启动条件 (Spawn)
  def startWhen(cond: Bool): Unit = { startSignal = cond }
  
  // 设置强杀条件 (Kill -9)
  // 优先级最高，一旦满足，下一拍 PC 归零，active 变 false
  def abortWhen(cond: Bool): Unit = { abortSignal = cond }
  
  // 设置暂停条件 (Context Switch / Wait)
  // 满足时，保持 PC 不变，保持输出不变
  def pauseWhen(cond: Bool): Unit = { pauseSignal = cond }

  // 状态查询
  def isRunning: Bool = active



  def driveManaged[T <: Data](signal: T, idleValue: T, activeDefault: T): T = {
    // 只有当线程 Active 时，才使用 activeDefault 或线程内部的值
    // 否则回退到 idleValue
    val threadDriver = WireInit(activeDefault)

    ownedSignals.add(threadDriver)
    signal := Mux(active, threadDriver, idleValue)
    threadDriver
  }

  def write[T <: Data](target: T, value: T): Unit = {
    if (!ownedSignals.contains(target)) {
       throw new Exception(s"[Error] Thread '$name' trying to drive unmanaged signal: $target")
    }
    target := value
  }



  def entry(block: => Unit): Unit = {

    block
    val totalSteps = steps.length
    

    if (totalSteps <= 1) {
      pcEntity = WireInit(0.U)
      isCombinational = true
      if (totalSteps == 1) steps(0)()
      globals.foreach(_())
      
    } else {

      val width = log2Ceil(totalSteps)
      val pcReg = RegInit(0.U(width.W))
      pcEntity = pcReg
      


      when (abortSignal) {
        active := false.B
        pcReg  := 0.U
      }
      // 优先级 2: 正常运行逻辑
      .elsewhen (active) {
        // 如果没有暂停，则执行
        when (!pauseSignal) {
          // 默认 PC 自增 (Fetch Next Instruction)
          pcReg := pcReg + 1.U
          
          // 执行当前 Step 的逻辑
          for ((func, idx) <- steps.zipWithIndex) {
            when (pcReg === idx.U) {
              func()
            }
          }
          
          // 边界检查：如果跑飞了，自动停机 (或者你可以改为循环)
          when (pcReg >= totalSteps.U) {
            // 默认行为：跑完所有 Step 后自动退出 (Exit)
            // 用户可以在最后一个 Step 修改 PC 来实现 Loop
            active := false.B
            pcReg := 0.U
          }
        }
      }
      // 优先级 3: 启动 (Spawn)
      .otherwise { // !active
        when (startSignal) {
          active := true.B
          pcReg  := 0.U
        }
      }

      
      globals.foreach(_())

    }
  }


  def Exit(): Unit = {
    active := false.B
    pcEntity := 0.U
  }
  
  // 新增：循环指令
  def Loop(): Unit = {
    pcEntity := 0.U
  }
  

  def Step(block: => Unit): Unit = {
    steps += { () => block }
  }
  
  def Label: UInt = steps.length.U


  def Global(block: => Unit): Unit = {
    globals += { () => block }
  }

  def Call[T <: Data, R <: Data](func: HwFunction[T, R], input: T): R = {

    val resultWire = Wire(chiselTypeOf(func.ret))
    resultWire := DontCare 

    steps += { () => 
      val latch = Reg(chiselTypeOf(func.ret))
      
      func.args   := input
      func.enable := true.B
      
      if (!isCombinational)
        pcEntity := pcEntity 
      
      when (func.done) {
        latch := func.ret
        if (!isCombinational)
          pcEntity := pcEntity + 1.U
      }
      resultWire := latch
    }
    resultWire
  }
}

class XIPOp extends Bundle {
  val addr  = UInt(32.W)
  val wdata = UInt(32.W)
  val write = Bool()
  val strb  = UInt(4.W) 
}

// 伴生对象，辅助构建 Wire
object XIPOp {
  def apply(addr: UInt, wdata: UInt, write: Bool = true.B, strb: UInt = "hF".U) = {
    val res = Wire(new XIPOp)
    res.addr  := addr
    res.wdata := wdata
    res.write := write
    res.strb  := strb
    res
  } 
}




class APBSPI(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
  val node = APBSlaveNode(Seq(APBSlavePortParameters(
    Seq(APBSlaveParameters(
      address       = address,
      executable    = true,
      supportsRead  = true,
      supportsWrite = true)),
    beatBytes  = 4)))

  lazy val module = new Impl

  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)
    val spi_bundle = IO(new SPIIO)
    val mspi = Module(new spi_top_apb)
    

    val mspi_proxy = Wire(new APBBundle(in.params))
    

    mspi.io.clock := clock
    mspi.io.reset := reset
    mspi.io.in <> mspi_proxy
    spi_bundle <> mspi.io.spi

    // CPU 直连模式下的输入连接 (总是连接，但会被 Driver 覆盖)
    mspi_proxy.paddr   := in.paddr
    mspi_proxy.pwdata  := in.pwdata
    mspi_proxy.pwrite  := in.pwrite
    mspi_proxy.psel    := in.psel 
    mspi_proxy.penable := in.penable
    mspi_proxy.pprot   := in.pprot
    mspi_proxy.pstrb   := in.pstrb


    class XIPAPBDriver extends HwFunction(new XIPOp, UInt(32.W)) {
      // 捕获 mspi_proxy
      val apb = mspi_proxy

      override def impl(): Unit = {
        val sSetup :: sAccess :: Nil = Enum(2)
        val state = RegInit(sSetup)
        
        // 默认返回值：本身属性，不在when中写
        ret := 0.U
        done := false.B

        when (enable) {
          switch (state) {
            is (sSetup) {
              state := sAccess
            }
            is (sAccess) {
              when (apb.pready) {
                state := sSetup
                done  := true.B
                ret   := apb.prdata // 捕获数据
              }
            }
          }

          apb.paddr   := args.addr
          apb.pwdata  := args.wdata
          apb.pwrite  := args.write
          apb.pstrb   := args.strb
          apb.psel    := true.B
          apb.penable := (state === sAccess)
          when (done) {

            printf(p"Call Driver FUNC DONE: node: ${args}, ret: ${ret}, done: ${done}\n")
          }

        } .otherwise {
          state := sSetup
        }
      }
    }

    val xipDriver = new XIPAPBDriver()
    xipDriver.impl()


    


    val xipThread = new HardwareThread("XIP_Core")
    
    // [关键点 1] 信号接管声明
    // 如果线程 Active，这些信号由线程控制；否则直通 mspi_proxy (CPU直接访问模式)
    // 这里的 default 值是在 XIP 运行期间的默认值
    val pReadyProxy = xipThread.driveManaged(in.pready,  mspi_proxy.pready,  false.B)
    val pDataProxy  = xipThread.driveManaged(in.prdata,  mspi_proxy.prdata,  0.U)
    val pErrProxy   = xipThread.driveManaged(in.pslverr, mspi_proxy.pslverr, false.B)



    val flashHit = in.psel && (in.paddr >= 0x30000000.U && in.paddr < 0x40000000.U)
    

    xipThread.startWhen(flashHit && in.penable)
    val pastRunnning = RegNext(xipThread.isRunning)
    
    
    

    xipThread.abortWhen(!in.psel)


    xipThread.entry {
      
      val spiBase = 0x10001000.U
      val targetAddr = in.paddr - 0x30000000.U

      xipThread.Call(xipDriver, XIPOp(spiBase + 0x00.U, 0.U))
      
      val cmdVal = (0x03.U(8.W) ## targetAddr(23, 0))
      xipThread.Call(xipDriver, XIPOp(spiBase + 0x04.U, cmdVal))

      xipThread.Call(xipDriver, XIPOp(spiBase + 0x14.U, 0.U))

      xipThread.Call(xipDriver, XIPOp(spiBase + 0x18.U, 1.U))
      xipThread.Call(xipDriver, XIPOp(spiBase + 0x10.U, 0x2540.U))



      val loopStartPC = xipThread.Label 
      

      val status = xipThread.Call(xipDriver, XIPOp(spiBase + 0x10.U, 0.U, write = false.B, strb = 0xF.U))
      
      xipThread.Step {
        when ((status & 0x100.U) =/= 0.U) {
          xipThread.pc := loopStartPC
        }
      }

      // --- Step 7: Read RX0 (Get Data) ---
      val finalData = xipThread.Call(xipDriver, XIPOp(spiBase + 0x00.U, 0.U, write = false.B, strb = 0xF.U))


      xipThread.Step {

        xipThread.write(pReadyProxy, true.B)
        xipThread.write(pDataProxy,  finalData)
        xipThread.write(pErrProxy,   false.B)


        xipThread.pc := xipThread.pc 
      }
      

      xipThread.Global {
        /*
        when(xipThread.isRunning) {
           printf(p"[XIP] PC=${xipThread.pc}\n")
        }
        */

        when (xipThread.isRunning && !pastRunnning) {
          printf("[DEBUG] [xipThread] xipThread ONLINE!!!\n")
          printf("targetAddr: %x\n", in.paddr - 0x30000000.U)
        }

        when (!xipThread.isRunning && pastRunnning) {
          printf("[DEBUG] [xipThread] xipThread OFFLINE!!!\n")
          printf("final data: %x\n", finalData)
        }
      }
    }
  }
}