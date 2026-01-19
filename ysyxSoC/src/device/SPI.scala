package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

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

class HardwareThread {
  // 1. 存储逻辑闭包
  private val steps = ArrayBuffer[() => Unit]()
  private val globals = ArrayBuffer[() => Unit]()
  
  // 2. 物理实体 (延迟创建)
  private var pcEntity: UInt = _
  private var isCombinational = false
  
  // 3. 供外部读取/写入 PC 的接口
  //    必须在 impl/entry 阶段调用，否则报错
  def pc: UInt = {
    require(pcEntity != null, "Error: Cannot access 'thread.pc' outside of Step/Call/Global logic! Hardware not generated yet.")
    pcEntity
  }

  // =================================================================
  // 入口函数
  // =================================================================
  def entry(block: => Unit): Unit = {
    // Phase 1: 收集 (Collection)
    // 执行用户的 block，把 Step/Call/Global 里的闭包收集起来
    block

    // Phase 2: 资源分配 (Allocation)
    val totalSteps = steps.length
    
    // 处理 Corner Case (0 或 1 步)
    if (totalSteps <= 1) {
      // 退化为 Wire，支持组合逻辑
      pcEntity = WireInit(0.U)
      isCombinational = true
      
      // 回放逻辑 (只执行第0步，如果有)
      if (totalSteps == 1) steps(0)()
      // 回放全局逻辑 (依然有效)
      globals.foreach(_())
      
    } else {
      // Normal Case: 创建寄存器
      val width = log2Ceil(totalSteps)
      // 我们创建一个 Reg，但类型标记为 UInt，方便统一处理
      val pcReg = RegInit(0.U(width.W))
      pcEntity = pcReg
      
      // Phase 3: 生成 Step 逻辑 (Generation)
      for ((stepFunc, idx) <- steps.zipWithIndex) {
        when (pcReg === idx.U) {
          // A. 默认行为：PC++
          pcReg := pcReg + 1.U
          
          // B. 用户 Step 行为 (可覆盖 A)
          stepFunc()
        }
      }
      
      // Phase 4: 生成 Global 逻辑 (Highest Priority)
      // C. 全局行为 (可覆盖 A 和 B)
      //    这在生成的 Verilog 中位于最下方，拥有最高优先级
      globals.foreach(_())

      // Phase 5: 循环回绕保护
      //    如果 Global 逻辑没有强制跳转，且 PC 超界，归零
      when (pcReg >= totalSteps.U) {
        pcReg := 0.U
      }
    }
  }

  def Step(block: => Unit): Unit = {
    steps += { () => block }
  }
  
  // 新增：全局逻辑
  // 这里的代码会在每个时钟周期执行，拥有最高修改 PC 的权限
  def Label: UInt = steps.length.U


  def Global(block: => Unit): Unit = {
    globals += { () => block }
  }

  def Call[T <: Data, R <: Data](func: HwFunction[T, R], input: T): R = {
    // 这里的 Wire 用于在生成前占位，将返回值传出去
    val resultWire = Wire(chiselTypeOf(func.ret))
    resultWire := DontCare 

    steps += { () => 
      // 在生成阶段创建 latch
      val latch = Reg(chiselTypeOf(func.ret))
      
      func.args   := input
      func.enable := true.B
      
      // 覆盖默认的 PC++，改为 Stall
      if (!isCombinational)
        pcEntity := pcEntity 
      
      when (func.done) {
        latch := func.ret
        // 完成后进位
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
        }
      }
    }


    class XIP extends HwFunction(UInt(32.W), UInt(32.W)) {
      // 资源引用
      val spiBase = 0x10001000
      val flashBase = 0x30000000

      override def impl(): Unit = {
        // 1. 实例化驱动
        val driver = new XIPAPBDriver()
        driver.impl() // 生成驱动电路

        // 2. 实例化线程
        val thread = new HardwareThread
        
        
        val targetAddr = RegEnable(args - flashBase.U, enable) 

        // 默认输出:这是Funciton本身的属性，不能留到when里面
        this.done := false.B
        this.ret  := 0.U


        when (enable) {

          // 4. 微码流程
          thread.entry {
            // 等待启动

            // Step 1: TX0 = 0
            thread.Call(driver, XIPOp((spiBase + 0x00).U, 0.U))
            
            // Step 2: TX1 = CMD(0x03) + ADDR
            val cmdVal = (0x03.U(8.W) ## targetAddr(23, 0))
            thread.Call(driver, XIPOp((spiBase + 0x04).U, cmdVal))

            // Step 3: DIV = 0
            thread.Call(driver, XIPOp((spiBase + 0x14).U, 0.U))

            // Step 4: SS = 1
            thread.Call(driver, XIPOp((spiBase + 0x18).U, 1.U))

            // Step 5: CTRL = Start (0x2540)
            thread.Call(driver, XIPOp((spiBase + 0x10).U, 0x2540.U))

            // Step 6: Polling Loop (while CTRL & 0x100)

            thread.Step {
              driver.enable := true.B
              driver.args := XIPOp((spiBase + 0x10).U, 0.U, write = false.B, strb = 0xF.U)
              val driverDone = driver.done
              val driverState = driver.ret

              thread.pc := thread.pc // MUST HOLD PC
              when (driverDone) {
                when ((driverState & 0x100.U) === 0.U ) {
                  thread.pc := thread.pc + 1.U
                }
              }
            }

            // Step 7: Read RX0
            val finalData = thread.Call(driver, XIPOp((spiBase + 0x00).U, 0.U, write = false.B, strb = 0.U))

            // Finish
            thread.Step {
              this.ret  := finalData
              this.done := true.B
            }
          }
        }

        // 5. 顶层仲裁与 Hold 逻辑
        // 当 XIP 忙碌时，接管对 CPU 的响应
        when (enable) {
          // 欺骗 CPU：让它一直等待 (Ready=0)，直到 done 为真
          in.pready := this.done
          in.prdata := this.ret
          in.pslverr := false.B

          
          // 注意：mspi_proxy 的驱动已经在 driver.impl() 里通过 when(enable) 处理了
          // 这里不需要再次赋值，driver 内部的逻辑会覆盖外面的默认逻辑
        }
      }
    }


    val flashHit = in.psel && (in.paddr >= 0x30000000.U && in.paddr < 0x40000000.U)

    // 1. 默认连接 (CPU 直连)

    mspi_proxy.paddr   := in.paddr
    mspi_proxy.pwdata  := in.pwdata
    mspi_proxy.pwrite  := in.pwrite
    mspi_proxy.psel    := in.psel && !flashHit 
    mspi_proxy.penable := in.penable
    mspi_proxy.pprot   := in.pprot
    mspi_proxy.pstrb   := in.pstrb


    in.pready := mspi_proxy.pready
    in.prdata := mspi_proxy.prdata
    in.pslverr := mspi_proxy.pslverr








    // 2. 实例化 XIP 逻辑
    val xipFunc = new XIP
    xipFunc.impl()

    val xipThread = new HardwareThread
    when(flashHit && in.penable) {
      xipThread.entry {
        xipThread.Call(xipFunc, in.paddr)
      }
    }


  }
}