error id: 53B28CAA7E531C9C5D7E9F9A9B798411
file://<WORKSPACE>/src/device/SPI.scala
### java.lang.IndexOutOfBoundsException: -1 is out of bounds (min 0, max 2)

occurred in the presentation compiler.



action parameters:
offset: 6982
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

          printf(p"Call Driver FUNC: addr: ${args}, ret: ${re@@}")
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

    // CPU 直连模式下的输入连接 (总是连接，但会被 Driver 覆盖)
    mspi_proxy.paddr   := in.paddr
    mspi_proxy.pwdata  := in.pwdata
    mspi_proxy.pwrite  := in.pwrite
    mspi_proxy.psel    := in.psel
    mspi_proxy.penable := in.penable
    mspi_proxy.pprot   := in.pprot
    mspi_proxy.pstrb   := in.pstrb

    // [关键点 2] 生命周期配置
    val flashHit = in.psel && (in.paddr >= 0x30000000.U && in.paddr < 0x40000000.U)
    
    // Start: 当访问 Flash 区域且总线 Enable 时，线程接管
    xipThread.startWhen(flashHit && in.penable)
    
    // Abort: 只要 CPU 撤销 psel，立刻强杀线程，复位状态
    // 这解决了死锁问题：Master 决定何时结束
    xipThread.abortWhen(!in.psel)


    // [关键点 3] 线程逻辑入口
    xipThread.entry {
      
      // 常量定义
      val spiBase = 0x10001000.U
      val targetAddr = in.paddr - 0x30000000.U

      // --- Step 1: TX0 = 0 (Select Slave) ---
      xipThread.Call(xipDriver, XIPOp(spiBase + 0x00.U, 0.U))
      
      // --- Step 2: TX1 = CMD(0x03) + ADDR ---
      val cmdVal = (0x03.U(8.W) ## targetAddr(23, 0))
      xipThread.Call(xipDriver, XIPOp(spiBase + 0x04.U, cmdVal))

      // --- Step 3: DIV = 0 ---
      xipThread.Call(xipDriver, XIPOp(spiBase + 0x14.U, 0.U))

      // --- Step 4: SS = 1 ---
      xipThread.Call(xipDriver, XIPOp(spiBase + 0x18.U, 1.U))

      // --- Step 5: CTRL = Start (0x2540) ---
      xipThread.Call(xipDriver, XIPOp(spiBase + 0x10.U, 0x2540.U))

      // --- Step 6: Polling Loop (Check Status) ---
      // 使用 Label 标记循环起点
      val loopStartPC = xipThread.Label 
      
      // 读取状态寄存器
      val status = xipThread.Call(xipDriver, XIPOp(spiBase + 0x10.U, 0.U, write = false.B, strb = 0xF.U))
      
      xipThread.Step {
        when ((status & 0x100.U) =/= 0.U) {
          xipThread.pc := loopStartPC
        }
      }

      // --- Step 7: Read RX0 (Get Data) ---
      val finalData = xipThread.Call(xipDriver, XIPOp(spiBase + 0x00.U, 0.U, write = false.B, strb = 0.U))

      // --- Step 8: Handshake & Finish ---
      xipThread.Step {
        // 使用 write 安全驱动托管信号
        xipThread.write(pReadyProxy, true.B)
        xipThread.write(pDataProxy,  finalData)
        xipThread.write(pErrProxy,   false.B)

        // 死循环保持输出，等待 abortWhen(!in.psel) 来杀死线程
        xipThread.pc := xipThread.pc 
      }
      
      // Global Debug
      xipThread.Global {
        when(xipThread.isRunning) {
           printf(p"[XIP] PC=${xipThread.pc}\n")
        }
      }
    }
  }
}
```


presentation compiler configuration:
Scala version: 2.13.14
Classpath:
<WORKSPACE>/.bloop/out/ysyxsoc/bloop-bsp-clients-classes/classes-Metals-WnXZU_0lQoyNAC4sG050CA== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.11.1/semanticdb-javac-0.11.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/chisel_2.13/7.0.0-M2/chisel_2.13-7.0.0-M2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.14/scala-library-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/mainargs_2.13/0.5.4/mainargs_2.13-0.5.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-jackson_2.13/4.0.6/json4s-jackson_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.14/scala-reflect-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/sourcecode_2.13/0.3.1/sourcecode_2.13-0.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.13/4.1.0/scopt_2.13-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native_2.13/4.0.7/json4s-native_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-text/1.12.0/commons-text-1.12.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/github/alexarchambault/data-class_2.13/0.2.6/data-class_2.13-0.2.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/os-lib_2.13/0.10.0/os-lib_2.13-0.10.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-parallel-collections_2.13/1.0.4/scala-parallel-collections_2.13-1.0.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle_2.13/3.3.0/upickle_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/firtool-resolver_2.13/2.0.0/firtool-resolver_2.13-2.0.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-collection-compat_2.13/2.11.0/scala-collection-compat_2.13-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-core_2.13/4.0.7/json4s-core_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-jackson-core_2.13/4.0.6/json4s-jackson-core_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native-core_2.13/4.0.7/json4s-native-core_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/geny_2.13/1.1.0/geny_2.13-1.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/ujson_2.13/3.3.0/ujson_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upack_2.13/3.3.0/upack_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-implicits_2.13/3.3.0/upickle-implicits_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.13/2.2.0/scala-xml_2.13-2.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-ast_2.13/4.0.7/json4s-ast_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-scalap_2.13/4.0.7/json4s-scalap_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/thoughtworks/paranamer/paranamer/2.8/paranamer-2.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.12.7/jackson-databind-2.12.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-core_2.13/3.3.0/upickle-core_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.12.7/jackson-annotations-2.12.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.12.7/jackson-core-2.12.7.jar [exists ], <WORKSPACE>/rocket-chip/dependencies/cde/cde/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.cde/bloop-bsp-clients-classes/classes-Metals-WnXZU_0lQoyNAC4sG050CA== [exists ], <WORKSPACE>/rocket-chip/dependencies/diplomacy/diplomacy/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.diplomacy/bloop-bsp-clients-classes/classes-Metals-WnXZU_0lQoyNAC4sG050CA== [exists ], <WORKSPACE>/rocket-chip/dependencies/hardfloat/hardfloat/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.hardfloat/bloop-bsp-clients-classes/classes-Metals-WnXZU_0lQoyNAC4sG050CA== [exists ], <WORKSPACE>/rocket-chip/macros/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.macros/bloop-bsp-clients-classes/classes-Metals-WnXZU_0lQoyNAC4sG050CA== [exists ], <WORKSPACE>/rocket-chip/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip/bloop-bsp-clients-classes/classes-Metals-WnXZU_0lQoyNAC4sG050CA== [exists ], <WORKSPACE>/compile-resources [missing ]
Options:
-language:reflectiveCalls -Ymacro-annotations -Ytasty-reader -Yrangepos -Xplugin-require:semanticdb




#### Error stacktrace:

```
scala.collection.mutable.ArrayBuffer.apply(ArrayBuffer.scala:102)
	scala.reflect.internal.Types$Type.findMemberInternal$1(Types.scala:1030)
	scala.reflect.internal.Types$Type.findMember(Types.scala:1035)
	scala.reflect.internal.Types$Type.memberBasedOnName(Types.scala:661)
	scala.reflect.internal.Types$Type.member(Types.scala:625)
	scala.reflect.internal.Types$Type.packageObject(Types.scala:637)
	scala.reflect.internal.tpe.TypeMaps$AsSeenFromMap.<init>(TypeMaps.scala:419)
	scala.reflect.internal.Types$Type.asSeenFrom(Types.scala:692)
	scala.reflect.internal.Types$Type.computeMemberType(Types.scala:728)
	scala.reflect.internal.Types$Type.memberType(Types.scala:721)
	scala.tools.nsc.typechecker.Implicits$ImplicitInfo.tpe(Implicits.scala:246)
	scala.tools.nsc.typechecker.Implicits$ImplicitInfo.computeErroneous(Implicits.scala:281)
	scala.tools.nsc.typechecker.Implicits$ImplicitInfo.isCyclicOrErroneous(Implicits.scala:276)
	scala.tools.nsc.typechecker.Implicits$ImplicitSearch$ImplicitComputation.isIneligible(Implicits.scala:1042)
	scala.tools.nsc.typechecker.Implicits$ImplicitSearch$ImplicitComputation.survives(Implicits.scala:1051)
	scala.tools.nsc.typechecker.Implicits$ImplicitSearch$ImplicitComputation.eligibleNew(Implicits.scala:1131)
	scala.tools.nsc.typechecker.Implicits$ImplicitSearch$ImplicitComputation.<init>(Implicits.scala:1185)
	scala.tools.nsc.typechecker.Implicits$ImplicitSearch.searchImplicit(Implicits.scala:1319)
	scala.tools.nsc.typechecker.Implicits$ImplicitSearch.bestImplicit(Implicits.scala:1716)
	scala.tools.nsc.typechecker.Implicits.inferImplicit1(Implicits.scala:112)
	scala.tools.nsc.typechecker.Implicits.inferImplicit(Implicits.scala:91)
	scala.tools.nsc.typechecker.Implicits.inferImplicit$(Implicits.scala:88)
	scala.meta.internal.pc.MetalsGlobal$MetalsInteractiveAnalyzer.inferImplicit(MetalsGlobal.scala:85)
	scala.tools.nsc.typechecker.Implicits.inferImplicitFor(Implicits.scala:46)
	scala.tools.nsc.typechecker.Implicits.inferImplicitFor$(Implicits.scala:45)
	scala.meta.internal.pc.MetalsGlobal$MetalsInteractiveAnalyzer.inferImplicitFor(MetalsGlobal.scala:85)
	scala.tools.nsc.typechecker.Typers$Typer.applyImplicitArgs(Typers.scala:265)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$adapt$1(Typers.scala:883)
	scala.tools.nsc.typechecker.Typers$Typer.adaptToImplicitMethod$1(Typers.scala:503)
	scala.tools.nsc.typechecker.Typers$Typer.adapt(Typers.scala:1351)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6276)
	scala.tools.nsc.typechecker.Typers$Typer.typedStat$1(Typers.scala:6339)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typedStats$9(Typers.scala:3539)
	scala.tools.nsc.typechecker.Typers$Typer.typedStats(Typers.scala:3539)
	scala.tools.nsc.typechecker.Typers$Typer.typedTemplate(Typers.scala:2144)
	scala.tools.nsc.typechecker.Typers$Typer.typedClassDef(Typers.scala:1982)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6168)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Typers$Typer.typedStat$1(Typers.scala:6339)
	scala.tools.nsc.typechecker.Typers$Typer.$anonfun$typedStats$9(Typers.scala:3539)
	scala.tools.nsc.typechecker.Typers$Typer.typedStats(Typers.scala:3539)
	scala.tools.nsc.typechecker.Typers$Typer.typedPackageDef$1(Typers.scala:5844)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6171)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Analyzer$typerFactory$TyperPhase.apply(Analyzer.scala:125)
	scala.tools.nsc.Global$GlobalPhase.applyPhase(Global.scala:481)
	scala.tools.nsc.interactive.Global$TyperRun.applyPhase(Global.scala:1369)
	scala.tools.nsc.interactive.Global$TyperRun.typeCheck(Global.scala:1362)
	scala.tools.nsc.interactive.Global.typeCheck(Global.scala:680)
	scala.meta.internal.pc.Compat.$anonfun$runOutline$1(Compat.scala:74)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:619)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:617)
	scala.collection.AbstractIterable.foreach(Iterable.scala:935)
	scala.meta.internal.pc.Compat.runOutline(Compat.scala:66)
	scala.meta.internal.pc.Compat.runOutline(Compat.scala:35)
	scala.meta.internal.pc.Compat.runOutline$(Compat.scala:33)
	scala.meta.internal.pc.MetalsGlobal.runOutline(MetalsGlobal.scala:39)
	scala.meta.internal.pc.ScalaCompilerWrapper.compiler(ScalaCompilerAccess.scala:18)
	scala.meta.internal.pc.ScalaCompilerWrapper.compiler(ScalaCompilerAccess.scala:13)
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

java.lang.IndexOutOfBoundsException: -1 is out of bounds (min 0, max 2)