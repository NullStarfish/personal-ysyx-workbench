error id: E7A5985F4582A8A9FBA994B617AB2763
file://<WORKSPACE>/src/device/SPI.scala
### java.lang.IndexOutOfBoundsException: -1 is out of bounds (min 0, max 0)

occurred in the presentation compiler.



action parameters:
offset: 8161
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




// ==============================================================================
// 4. APBSPI 模块实现
// ==============================================================================
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
    
    // 创建一个代理 Wire，它是 SPI 控制器的唯一入口
    val mspi_proxy = Wire(new APBBundle(in.params))
    
    // 连接 BlackBox
    mspi.io.clock := clock
    mspi.io.reset := reset
    mspi.io.in <> mspi_proxy
    spi_bundle <> mspi.io.spi

    // ----------------------------------------------------------
    // 定义底层的驱动函数 (XIPAPBDriver)
    // ----------------------------------------------------------
    class XIPAPBDriver extends HwFunction(new XIPOp, UInt(32.W)) {
      // 捕获 mspi_proxy
      val apb = mspi_proxy

      override def impl(): Unit = {
        val sSetup :: sAccess :: Nil = Enum(2)
        val state = RegInit(sSetup)
        
        // 默认返回值
        ret := 0.U
        done := false.B

        // 只有当 HwThread 激活我时，我才工作
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

          // 驱动总线 (Override 默认逻辑)
          apb.paddr   := args.addr
          apb.pwdata  := args.wdata
          apb.pwrite  := args.write
          apb.pstrb   := args.strb
          apb.psel    := true.B
          apb.penable := (state === sAccess)
        }
      }
    }

    // ----------------------------------------------------------
    // 定义高层的 XIP 逻辑 (The Sequence)
    // ----------------------------------------------------------
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
        
        // 3. 状态管理
        val busy = RegInit(false.B)
        when(enable) { busy := true.B }
        
        // 捕获目标地址 (去掉偏移)
        val targetAddr = RegEnable(args - flashBase.U, enable) 

        // 默认输出
        this.done := false.B
        this.ret  := 0.U


        when (busy) {

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
              busy      := false.B
            }
          }
        }

        // 5. 顶层仲裁与 Hold 逻辑
        // 当 XIP 忙碌时，接管对 CPU 的响应
        when (busy) {
          // 欺骗 CPU：让它一直等待 (Ready=0)，直到 done 为真
          in.pready := this.done
          in.prdata := this.ret
          in.pslverr := false.B

          printf("[DEBUG] [XIP]: thread.pc : %d\n", thread.pc)
          
          // 注意：mspi_proxy 的驱动已经在 driver.impl() 里通过 when(enable) 处理了
          // 这里不需要再次赋值，driver 内部的逻辑会覆盖外面的默认逻辑
        }
      }
    }

    // ----------------------------------------------------------
    // 顶层连接逻辑 (Main)
    // ----------------------------------------------------------

    // 判断是否命中 Flash 区域
    val flashHit = in.psel && (in.paddr >= 0x30000000.U && in.paddr < 0x40000000.U)

    // 1. 默认连接 (CPU 直连)
    // 只有当不是 Flash 访问时，CPU 才能控制 SPI 控制器
    mspi_proxy.paddr   := in.paddr
    mspi_proxy.pwdata  := in.pwdata
    mspi_proxy.pwrite  := in.pwrite
    mspi_proxy.psel    := in.psel && !flashHit // 关键：如果是 Flash 访问，断开直连
    mspi_proxy.penable := in.penable
    mspi_proxy.pprot   := in.pprot
    mspi_proxy.pstrb   := in.pstrb

    // CPU 默认读到的数据
    in.pready := mspi_proxy.pready
    in.prdata := mspi_proxy.prdata
    in.pslverr := mspi_proxy.pslverr








    // 2. 实例化 XIP 逻辑
    val xipFunc = new XIP
    xipFunc.impl()

    val xipThread = new HardwareThread

    xipThread.entry {
      



      when (!(f@@))
    }
    /*
    // 3. 启动 XIP
    // 参数：直接把 CPU 的地址传进去
    // 返回值：XIP 模块会自动处理 ready/valid 握手
    xip.args := in.paddr
    
    // 只有当命中 Flash 且 CPU 真正发起请求时，才启动 XIP
    // 注意：in.penable 确保了这是一个确定的 APB 传输
    xip.enable := flashHit && in.penable

    // 4. 生成 XIP 硬件
    // 这里面包含了：
    //   - HardwareThread 的逻辑
    //   - XIPAPBDriver 的逻辑
    //   - 对 mspi_proxy 的 Override (当 busy 时)
    //   - 对 in.pready/prdata 的 Override (当 busy 时)
    xip.impl()
    */

  }
}
```


presentation compiler configuration:
Scala version: 2.13.14
Classpath:
<WORKSPACE>/.bloop/out/ysyxsoc/bloop-bsp-clients-classes/classes-Metals-nWcVXkWaR4qQd8nLuhjX4w== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.11.1/semanticdb-javac-0.11.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/chisel_2.13/7.0.0-M2/chisel_2.13-7.0.0-M2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.14/scala-library-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/mainargs_2.13/0.5.4/mainargs_2.13-0.5.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-jackson_2.13/4.0.6/json4s-jackson_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.14/scala-reflect-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/sourcecode_2.13/0.3.1/sourcecode_2.13-0.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.13/4.1.0/scopt_2.13-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native_2.13/4.0.7/json4s-native_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-text/1.12.0/commons-text-1.12.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/github/alexarchambault/data-class_2.13/0.2.6/data-class_2.13-0.2.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/os-lib_2.13/0.10.0/os-lib_2.13-0.10.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-parallel-collections_2.13/1.0.4/scala-parallel-collections_2.13-1.0.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle_2.13/3.3.0/upickle_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/firtool-resolver_2.13/2.0.0/firtool-resolver_2.13-2.0.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-collection-compat_2.13/2.11.0/scala-collection-compat_2.13-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-core_2.13/4.0.7/json4s-core_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-jackson-core_2.13/4.0.6/json4s-jackson-core_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native-core_2.13/4.0.7/json4s-native-core_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/geny_2.13/1.1.0/geny_2.13-1.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/ujson_2.13/3.3.0/ujson_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upack_2.13/3.3.0/upack_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-implicits_2.13/3.3.0/upickle-implicits_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.13/2.2.0/scala-xml_2.13-2.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-ast_2.13/4.0.7/json4s-ast_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-scalap_2.13/4.0.7/json4s-scalap_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/thoughtworks/paranamer/paranamer/2.8/paranamer-2.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.12.7/jackson-databind-2.12.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-core_2.13/3.3.0/upickle-core_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.12.7/jackson-annotations-2.12.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.12.7/jackson-core-2.12.7.jar [exists ], <WORKSPACE>/rocket-chip/dependencies/cde/cde/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.cde/bloop-bsp-clients-classes/classes-Metals-nWcVXkWaR4qQd8nLuhjX4w== [exists ], <WORKSPACE>/rocket-chip/dependencies/diplomacy/diplomacy/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.diplomacy/bloop-bsp-clients-classes/classes-Metals-nWcVXkWaR4qQd8nLuhjX4w== [exists ], <WORKSPACE>/rocket-chip/dependencies/hardfloat/hardfloat/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.hardfloat/bloop-bsp-clients-classes/classes-Metals-nWcVXkWaR4qQd8nLuhjX4w== [exists ], <WORKSPACE>/rocket-chip/macros/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.macros/bloop-bsp-clients-classes/classes-Metals-nWcVXkWaR4qQd8nLuhjX4w== [exists ], <WORKSPACE>/rocket-chip/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip/bloop-bsp-clients-classes/classes-Metals-nWcVXkWaR4qQd8nLuhjX4w== [exists ], <WORKSPACE>/compile-resources [missing ]
Options:
-language:reflectiveCalls -Ymacro-annotations -Ytasty-reader -Yrangepos -Xplugin-require:semanticdb




#### Error stacktrace:

```
scala.collection.mutable.ArrayBuffer.apply(ArrayBuffer.scala:102)
	scala.reflect.internal.Trees$TreeContextApiImpl.onlyChild(Trees.scala:198)
	scala.reflect.internal.Positions.atPos(Positions.scala:408)
	scala.reflect.internal.Positions.atPos$(Positions.scala:399)
	scala.reflect.internal.SymbolTable.atPos(SymbolTable.scala:28)
	scala.tools.nsc.ast.parser.Parsers$Parser.atPos(Parsers.scala:807)
	scala.tools.nsc.ast.parser.Parsers$Parser.atPos(Parsers.scala:805)
	scala.tools.nsc.ast.parser.Parsers$Parser.simpleExprRest(Parsers.scala:1974)
	scala.tools.nsc.ast.parser.Parsers$Parser.simpleExpr(Parsers.scala:1942)
	scala.tools.nsc.ast.parser.Parsers$Parser.prefixExpr(Parsers.scala:1897)
	scala.tools.nsc.ast.parser.Parsers$Parser.postfixExpr(Parsers.scala:1870)
	scala.tools.nsc.ast.parser.Parsers$Parser.parseOther$1(Parsers.scala:1765)
	scala.tools.nsc.ast.parser.Parsers$Parser.expr0(Parsers.scala:1821)
	scala.tools.nsc.ast.parser.Parsers$Parser.expr(Parsers.scala:1667)
	scala.tools.nsc.ast.parser.Parsers$Parser.statement(Parsers.scala:1640)
	scala.tools.nsc.ast.parser.Parsers$Parser.$anonfun$blockStatSeq$1(Parsers.scala:3616)
	scala.tools.nsc.ast.parser.Parsers$Parser.checkNoEscapingPlaceholders(Parsers.scala:499)
	scala.tools.nsc.ast.parser.Parsers$Parser.blockStatSeq(Parsers.scala:3597)
	scala.tools.nsc.ast.parser.Parsers$Parser.block(Parsers.scala:2038)
	scala.tools.nsc.ast.parser.Parsers$Parser.$anonfun$blockExpr$1(Parsers.scala:2025)
	scala.tools.nsc.ast.parser.ParsersCommon$ParserCommon.inBraces(Parsers.scala:66)
	scala.tools.nsc.ast.parser.Parsers$Parser.blockExpr(Parsers.scala:2024)
	scala.tools.nsc.ast.parser.Parsers$Parser.simpleExpr(Parsers.scala:1930)
	scala.tools.nsc.ast.parser.Parsers$Parser.prefixExpr(Parsers.scala:1897)
	scala.tools.nsc.ast.parser.Parsers$Parser.postfixExpr(Parsers.scala:1870)
	scala.tools.nsc.ast.parser.Parsers$Parser.parseOther$1(Parsers.scala:1765)
	scala.tools.nsc.ast.parser.Parsers$Parser.expr0(Parsers.scala:1821)
	scala.tools.nsc.ast.parser.Parsers$Parser.expr(Parsers.scala:1667)
	scala.tools.nsc.ast.parser.Parsers$Parser.expr(Parsers.scala:1665)
	scala.tools.nsc.ast.parser.Parsers$Parser.funDefRest(Parsers.scala:3083)
	scala.tools.nsc.ast.parser.Parsers$Parser.funDefOrDcl(Parsers.scala:3040)
	scala.tools.nsc.ast.parser.Parsers$Parser.defOrDcl(Parsers.scala:2894)
	scala.tools.nsc.ast.parser.Parsers$Parser.nonLocalDefOrDcl(Parsers.scala:2906)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anonfun$templateStat$1.$anonfun$applyOrElse$2(Parsers.scala:3525)
	scala.tools.nsc.ast.parser.Parsers$Parser.joinComment(Parsers.scala:800)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anonfun$templateStat$1.applyOrElse(Parsers.scala:3525)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anonfun$templateStat$1.applyOrElse(Parsers.scala:3520)
	scala.tools.nsc.ast.parser.Parsers$Parser.statSeq(Parsers.scala:3452)
	scala.tools.nsc.ast.parser.Parsers$Parser.$anonfun$templateStats$1(Parsers.scala:3519)
	scala.tools.nsc.ast.parser.Parsers$Parser.checkNoEscapingPlaceholders(Parsers.scala:499)
	scala.tools.nsc.ast.parser.Parsers$Parser.templateStats(Parsers.scala:3519)
	scala.tools.nsc.ast.parser.Parsers$Parser.templateStatSeq(Parsers.scala:3506)
	scala.tools.nsc.ast.parser.Parsers$Parser.templateBody(Parsers.scala:3411)
	scala.tools.nsc.ast.parser.Parsers$Parser.templateBodyOpt(Parsers.scala:3418)
	scala.tools.nsc.ast.parser.Parsers$Parser.templateOpt(Parsers.scala:3384)
	scala.tools.nsc.ast.parser.Parsers$Parser.$anonfun$classDef$1(Parsers.scala:3241)
	scala.tools.nsc.ast.parser.Parsers$Parser.savingClassContextBounds(Parsers.scala:344)
	scala.tools.nsc.ast.parser.Parsers$Parser.classDef(Parsers.scala:3227)
	scala.tools.nsc.ast.parser.Parsers$Parser.tmplDef(Parsers.scala:3195)
	scala.tools.nsc.ast.parser.Parsers$Parser.topLevelTmplDef(Parsers.scala:3180)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anonfun$topStat$1.$anonfun$applyOrElse$1(Parsers.scala:3475)
	scala.tools.nsc.ast.parser.Parsers$Parser.joinComment(Parsers.scala:800)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anonfun$topStat$1.applyOrElse(Parsers.scala:3475)
	scala.tools.nsc.ast.parser.Parsers$Parser$$anonfun$topStat$1.applyOrElse(Parsers.scala:3468)
	scala.tools.nsc.ast.parser.Parsers$Parser.statSeq(Parsers.scala:3452)
	scala.tools.nsc.ast.parser.Parsers$Parser.topStatSeq(Parsers.scala:3467)
	scala.tools.nsc.ast.parser.Parsers$Parser.topstats$1(Parsers.scala:3664)
	scala.tools.nsc.ast.parser.Parsers$Parser.topstats$1(Parsers.scala:3656)
	scala.tools.nsc.ast.parser.Parsers$Parser.$anonfun$compilationUnit$1(Parsers.scala:3670)
	scala.tools.nsc.ast.parser.Parsers$Parser.checkNoEscapingPlaceholders(Parsers.scala:499)
	scala.tools.nsc.ast.parser.Parsers$Parser.compilationUnit(Parsers.scala:3634)
	scala.tools.nsc.ast.parser.Parsers$SourceFileParser.$anonfun$parseStartRule$1(Parsers.scala:168)
	scala.tools.nsc.ast.parser.Parsers$Parser.$anonfun$parse$1(Parsers.scala:369)
	scala.tools.nsc.ast.parser.Parsers$Parser.parseRule(Parsers.scala:362)
	scala.tools.nsc.ast.parser.Parsers$Parser.parse(Parsers.scala:369)
	scala.tools.nsc.ast.parser.Parsers$UnitParser.$anonfun$smartParse$1(Parsers.scala:265)
	scala.tools.nsc.ast.parser.Parsers$UnitParser.smartParse(Parsers.scala:240)
	scala.tools.nsc.ast.parser.SyntaxAnalyzer.scala$tools$nsc$ast$parser$SyntaxAnalyzer$$initialUnitBody(SyntaxAnalyzer.scala:94)
	scala.tools.nsc.ast.parser.SyntaxAnalyzer$ParserPhase.apply(SyntaxAnalyzer.scala:106)
	scala.tools.nsc.Global$GlobalPhase.applyPhase(Global.scala:481)
	scala.tools.nsc.Global$Run.$anonfun$compileLate$2(Global.scala:1698)
	scala.tools.nsc.Global$Run.$anonfun$compileLate$2$adapted(Global.scala:1697)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:619)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:617)
	scala.collection.AbstractIterator.foreach(Iterator.scala:1303)
	scala.tools.nsc.Global$Run.compileLate(Global.scala:1697)
	scala.tools.nsc.interactive.Global.parseAndEnter(Global.scala:668)
	scala.tools.nsc.interactive.Global.typeCheck(Global.scala:678)
	scala.meta.internal.pc.WithCompilationUnit.<init>(WithCompilationUnit.scala:24)
	scala.meta.internal.pc.WithSymbolSearchCollector.<init>(PcCollector.scala:356)
	scala.meta.internal.pc.PcDocumentHighlightProvider.<init>(PcDocumentHighlightProvider.scala:12)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$documentHighlight$1(ScalaPresentationCompiler.scala:527)
	scala.meta.internal.pc.CompilerAccess.withSharedCompiler(CompilerAccess.scala:148)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withInterruptableCompiler$1(CompilerAccess.scala:92)
	scala.meta.internal.pc.CompilerAccess.$anonfun$onCompilerJobQueue$1(CompilerAccess.scala:209)
	scala.meta.internal.pc.CompilerJobQueue$Job.run(CompilerJobQueue.scala:152)
	java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
	java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
	java.base/java.lang.Thread.run(Thread.java:1583)
```
#### Short summary: 

java.lang.IndexOutOfBoundsException: -1 is out of bounds (min 0, max 0)