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

class HardwareThread {
  // 物理寄存器：程序计数器
  val pc = RegInit(0.U(32.W)) 
  
  // 编译期计数器：用来给每一行代码分配行号
  private var stepId = 0

  // 定义“入口”：也就是 main 函数
  def entry(block: => Unit): Unit = {
    block // 执行传入的代码块，生成所有硬件
    // 自动循环：最后一步跳回 0 (类似 while(1))
    when (pc === stepId.U) { pc := 0.U }
  }

  // 核心魔法：CALL 指令
  // 泛型 T: 参数类型, R: 返回值类型
  def Call[T <: Data, R <: Data](func: HwFunction[T, R], input: T): R = {
    val currentStep = stepId.U
    val resultReg = Reg(chiselTypeOf(func.ret)) // 这里的 Reg 相当于保存返回值的寄存器

    // 生成当前步骤的硬件逻辑
    when (pc === currentStep) {
      // 1. 传参 (a0)
      func.args := input
      // 2. 启动 (jal)
      func.enable := true.B
      
      // 3. 等待返回 (check ra/done)
      when (func.done) {
        resultReg := func.ret // 捕获返回值
        pc := pc + 1.U        // PC++，进入下一行代码
      }
    }

    // 编译期行号 + 1
    stepId += 1
    
    // 返回那个寄存器，供下一行代码使用
    resultReg
  }
  
  // 普通的顺序逻辑 (比如赋值操作)
  def Step(block: => Unit): Unit = {
    val currentStep = stepId.U
    when (pc === currentStep) {
      pc := pc + 1.U
      block
    }
    stepId += 1
  }
}


// 1. 定义操作指令包 (Bundle)
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
            val driverState = thread.Call(driver, XIPOp((spiBase + 0x10).U, 0.U, write = false.B, strb = 0xF.U))
            val isSpiBusy = (driver.ret & 0x100.U) =/= 0.U
            when (isSpiBusy) {
              thread.pc := thread.pc
            }
            // cover the pc update logic in thread.Call
            

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