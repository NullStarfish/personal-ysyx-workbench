error id: file://<WORKSPACE>/src/device/SPI.scala:
file://<WORKSPACE>/src/device/SPI.scala
empty definition using pc, found symbol in pc: 
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -chisel3/mspi.
	 -chisel3/mspi#
	 -chisel3/mspi().
	 -chisel3/util/mspi.
	 -chisel3/util/mspi#
	 -chisel3/util/mspi().
	 -freechips/rocketchip/amba/apb/mspi.
	 -freechips/rocketchip/amba/apb/mspi#
	 -freechips/rocketchip/amba/apb/mspi().
	 -freechips/rocketchip/diplomacy/mspi.
	 -freechips/rocketchip/diplomacy/mspi#
	 -freechips/rocketchip/diplomacy/mspi().
	 -freechips/rocketchip/util/mspi.
	 -freechips/rocketchip/util/mspi#
	 -freechips/rocketchip/util/mspi().
	 -mspi.
	 -mspi#
	 -mspi().
	 -scala/Predef.mspi.
	 -scala/Predef.mspi#
	 -scala/Predef.mspi().
offset: 3391
uri: file://<WORKSPACE>/src/device/SPI.scala
text:
```scala
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
      block
      pc := pc + 1.U
    }
    stepId += 1
  }
}


class XIPOp extends Bundle {
  val addr = UInt(32.W)
  val wdata = UInt(32.W)
  val write = Bool()
  val strb = UInt(4.W) 
}

object XIPOp {
  def apply(addr: UInt, wdata: UInt, write: UInt, strb: UInt) = {
    val res = Wire(new XIPOp)
    res.addr := addr
    res.wdata := wdata
    res.write := write
    res.strb := strb
    res
  } 
}


class XIPAPBDriver(implOuter: XIP) extends HwFunction(new XIPOp, UInt(0.W)) {
  val apb = implOuter.mspi.io.in

  override def impl(): Unit = {
    val sSetup :: sAccess :: Nil = Enum(2)
    val state = RegInit(sSetup)
    when (enable) { //NECESSARY
      switch (state) {
        is (sSetup) {
          state := sAccess
        }
        is (sAccess) {
          when (apb.pready) {
            state := sSetup
            done := true.B // THIS IS COMBINATIONAL
          }
        }
      }
    


      apb.paddr := args.addr
      apb.pstrb := args.strb
      apb.pwdata := args.wdata
      apb.psel := true.B
      apb.penable := (state === sAccess)
      

      


    }
  }
}




class XIP(implOuter: Impl) extends HwFunction(UInt(32.W), UInt(32.W)) {
  // 引用外部环境
  val in = implOuter.in 
  val mspi@@ = implOuter.mspi
  
  override def impl(): Unit = {



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
  //if hit flash, turn into XIP execute
  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)
    val spi_bundle = IO(new SPIIO)

    val mspi = Module(new spi_top_apb)
    val sNormal :: sXIP :: Nil = Enum(2)
    val state = RegInit(sNormal)

import chisel3._
import chisel3.util._

// 假设这些是外部定义的 Bundle，为了完整性我补在这里
// 实际使用时请替换为你项目中的定义
class APBBundle extends Bundle {
  val paddr   = Output(UInt(32.W))
  val psel    = Output(Bool())
  val penable = Output(Bool())
  val pwrite  = Output(Bool())
  val pwdata  = Output(UInt(32.W))
  val pstrb   = Output(UInt(4.W))
  val pready  = Input(Bool())
  val prdata  = Input(UInt(32.W))
  val pslverr = Input(Bool())
}

// ==============================================================================
// 1. BlockingProcedure 基类 (你提供的模板)
// ==============================================================================
abstract class BlockingProcedure(parentState: UInt) {
  // 这里的 busy 是当前过程的"运行中"标志
  val busy = RegInit(false.B)

  // 必须实现的具体逻辑
  // 输入 fire: 过程启动信号 (脉冲)
  // 输出 (done, data): done 为完成脉冲, data 为返回数据
  protected def logic(fire: Bool): (Bool, UInt)

  // 对外接口：像调用函数一样调用硬件
  // returnState: 执行完后，你想让父寄存器变成什么值？
  def run(returnState: UInt): UInt = {
    // 自动检测启动条件：当父状态机指向这里，且我不忙时
    // 利用上下文感知：调用者通常在 when(state === sXIP) 中调用
    val isCallerActive = true.B 
    val fire = !busy && isCallerActive

    when(fire) { busy := true.B }

    // 执行具体逻辑
    val (done, data) = logic(fire)

    // 魔法时刻：子对象直接控制父状态机的跳转！
    when(busy && done) {
      busy := false.B
      parentState := returnState // <--- 修改传入的父寄存器
    }

    data
  }
}

// ==============================================================================
// 2. DriveAPBProcedure 实现 (叶子节点过程)
//    它负责管理 APB 的 Setup/Access 握手
//    它的 Parent 是 XIP 的 upc (计数器)
// ==============================================================================
class DriveAPBProcedure(pc: UInt, apb: APBBundle) extends BlockingProcedure(pc) {
  
  // 定义输入接口 (Wire)，父过程在 run 之前需要给这些 Wire 赋值
  class Request extends Bundle {
    val addr  = UInt(32.W)
    val wdata = UInt(32.W)
    val write = Bool()
    val strb  = UInt(4.W)
  }
  // 使用 Wire 而不是 IO，因为这是类内部的互联
  val req = Wire(new Request)
  
  // 内部状态机 (用于 APB 时序)
  // 注意：BlockingProcedure 的 busy 已经表示了"是否在运行"，
  // 我们只需要区分 Setup 和 Access 阶段
  val sSetup :: sAccess :: Nil = Enum(2)
  val state = RegInit(sSetup)

  protected def logic(fire: Bool): (Bool, UInt) = {
    // 锁存请求参数，因为父过程的 opSeq 是组合逻辑，随 upc 变化
    val r_req = RegEnable(req, fire) 
    val activeReq = Mux(fire, req, r_req) // 优化：第一拍直接通过

    // 状态跳转逻辑
    switch(state) {
      is(sSetup) {
        state := sAccess // Setup 只有一拍
      }
      is(sAccess) {
        when(apb.pready) {
          state := sSetup // 准备下一次
        }
      }
    }
    
    // 复位状态机：当新的任务开始时，强制回到 Setup
    when(fire) { state := sSetup }

    // 驱动 APB 信号
    // 注意：这里的 activeReq 实际上被 busy 保护着，
    // 只有 run 被调用且 busy 为 true 时，这些赋值才有效（如果放在顶层 when 中）
    apb.paddr   := activeReq.addr
    apb.pwdata  := activeReq.wdata
    apb.pwrite  := activeReq.write
    apb.pstrb   := activeReq.strb
    
    // 根据 APB 协议生成 psel 和 penable
    // 只有在 busy 状态下才驱动
    apb.psel    := true.B 
    apb.penable := (state === sAccess)

    // 完成条件：处于 Access 阶段且收到 pready
    val done = (state === sAccess) && apb.pready
    
    (done, apb.prdata)
  }
}

// ==============================================================================
// 3. XIPProcedure 实现 (顶层过程)
//    它负责管理 XIP 的指令序列
//    它的 Parent 是全局的 state (sNormal/sXIP)
// ==============================================================================
class XIPProcedure(globalState: UInt, in: APBBundle, apb: APBBundle) extends BlockingProcedure(globalState) {
  val spiBase = 0x10001000
  
  // XIP 内部的状态寄存器 (Program Counter)
  // 这个寄存器将被传递给子过程 (driver) 进行修改
  val upc = RegInit(0.U(3.W))
  
  val targetAddr = Reg(UInt(32.W))
  val dataReg    = Reg(UInt(32.W))

  // 实例化子过程：DriveAPB
  // 关键点：我们将 upc 传给它，这意味着 driver.run() 会修改 upc
  val driver = new DriveAPBProcedure(upc, apb)

  // 辅助构建函数 (匿名 Bundle)
  def Op(addr: UInt, wdata: UInt, write: Bool = true.B, strb: UInt = "hF".U) = {
    val res = Wire(new Bundle {
      val addr  = chiselTypeOf(addr)
      val wdata = chiselTypeOf(wdata)
      val write = chiselTypeOf(write)
      val strb  = chiselTypeOf(strb)
    })
    res.addr  := addr
    res.wdata := wdata
    res.write := write
    res.strb  := strb
    res
  }

  protected def logic(fire: Bool): (Bool, UInt) = {
    
    // 1. 初始化逻辑
    when(fire) {
      upc := 0.U
      targetAddr := in.paddr - 0x30000000.U // 捕获地址
    }

    // 2. 指令序列定义
    val opSeq = VecInit(Seq(
      Op((spiBase + 0x00).U, 0x00.U), 
      Op((spiBase + 0x04).U, (0x03.U << 24) | (targetAddr & 0x00FFFFFF.U)),
      Op((spiBase + 0x14).U, 0.U),
      Op((spiBase + 0x18).U, 1.U),
      Op((spiBase + 0x10).U, 0x2540.U),          // Step 4: Start
      Op((spiBase + 0x10).U, 0.U, false.B, 0.U), // Step 5: Poll CTRL
      Op((spiBase + 0x00).U, 0.U, false.B, 0.U)  // Step 6: Read Data
    ))
    
    // 获取当前指令
    val currentOp = opSeq(upc)

    // 3. 连接参数给子过程
    // 在调用 run 之前，先把参数塞给 Wire
    driver.req.addr  := currentOp.addr
    driver.req.wdata := currentOp.wdata
    driver.req.write := currentOp.write
    driver.req.strb  := currentOp.strb

    // 4. 计算子过程完成后的跳转目标 (Next State Logic)
    // 默认情况：upc + 1
    // 特殊情况：Polling (Step 5)
    val nextUPC = Wire(UInt(3.W))
    val isPollingStep = (upc === 5.U)
    
    // 只有当 driver 返回数据时 (apb_done)，prdata 才有效
    // 我们需要检查 CTRL 寄存器的 Bit 8 (Busy 位)
    // readData 是 driver.run 的返回值
    // 注意：这里有一个小的时序环，但因为 update 发生在寄存器输入端，所以是合法的组合逻辑路径
    val readData = Wire(UInt(32.W)) 
    val isBusyBitSet = (readData & 0x100.U) =/= 0.U
    
    when (isPollingStep && isBusyBitSet) {
      nextUPC := upc // 忙？原地踏步，重试
    } .otherwise {
      nextUPC := upc + 1.U // 不忙，或者不是 polling 步？下一步
    }

    // 5. 运行子过程！(Magic Happens Here)
    // 这一行代码会：
    // - 启动 APB 传输
    // - 阻塞直到 pready
    // - 完成后，自动将 upc 更新为 nextUPC
    readData := driver.run(returnState = nextUPC)

    // 6. 判断整个序列是否完成
    // 当 upc 达到 6 且 driver 刚刚完成那一次传输时
    val seqDone = WireDefault(false.B)
    
    // driver.busy 从 true 变为 false 的瞬间，意味着一次传输完成，upc 即将更新
    // 我们检查更新后的值是否越界，或者检查当前步是否是最后一步且已完成
    // 更加简单的做法：检查 driver 是否在跑最后一步并且完成了
    when (upc === 6.U && !driver.busy) { 
        // 注意：这里逻辑要小心。driver.busy 拉低说明刚刚做完。
        // 因为 blockingProcedure 的特性，它会在做完时直接改 upc。
        // 所以我们可以在这里锁存数据
        seqDone := true.B
        dataReg := readData
    }
    
    // 这里的 seqDone 信号会告诉 XIPProcedure 的基类：
    // "我做完了，请把全局状态 state 改回 sNormal"
    (seqDone, dataReg)
  }
}

       
    val mspi_proxy = Wire(new APBBundle(in.params))

    //mspi_proxy := in //Default, execute will cover it
    /* this is not correct:
      compare:
        1. out := 1.U
        when (cond) {
        out := 2.U}
        
        2.when (cond) {
          out := 2.U
        } .otherwise {
          out := 1.U
        }

        the two forms are equal


        BUT!!!!!

        the driveAPB is a stateMACHINE!!!!
        when sIdle, it still holds the mspi_proxy        
         */
    mspi.io.in <> mspi_proxy
  

    val xip = new XIP



    
    //Shall we use flashData? Actually execute has sent flashData on APB
    


    val flashHit = in.psel && (in.paddr >= 0x30000000.U && in.paddr < 0x40000000.U)




    //val (xipDone, flashData) = xip.execute(state === sXIP, mspi_proxy)
    val xipDone = WireDefault(false.B)
    val flashData = WireDefault(0.U(32.W))
    switch(state) {
      is (sNormal) {
        when (flashHit && in.penable) {
          state := sXIP
        }
      }
      is (sXIP) {
        when (xipDone) {
          state := sNormal
        }
      }
    }
    mspi_proxy.paddr   := in.paddr
    mspi_proxy.pwdata  := in.pwdata
    mspi_proxy.pwrite  := in.pwrite
    mspi_proxy.psel    := in.psel && !flashHit // 默认屏蔽 Flash 区域
    mspi_proxy.penable := in.penable
    mspi_proxy.pprot   := in.pprot   // 解决初始化报错
    mspi_proxy.pstrb   := in.pstrb   // 解决初始化报错
    when(state === sXIP) {
      val (done, data) = xip.execute(true.B, mspi_proxy)
      xipDone := done
      flashData := data

    } .otherwise {

    }


    in.pready := Mux(flashHit, xipDone, mspi_proxy.pready)
    in.prdata := Mux(flashHit, flashData, mspi_proxy.prdata)


    mspi.io.clock := clock
    mspi.io.reset := reset
    



    spi_bundle <> mspi.io.spi

  }
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: 