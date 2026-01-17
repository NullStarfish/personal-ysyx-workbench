error id: F1516AAA6B3D9A9DDAD854C2C9857C6E
file://<WORKSPACE>/src/device/SPI.scala
### java.lang.IndexOutOfBoundsException: -1 is out of bounds (min 0, max 2)

occurred in the presentation compiler.



action parameters:
offset: 4814
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
    /* 
      apb: CPU interface
      mspi: SPI Controller:
        inside mspi, we have flash and other slaves mounted on 
        mspi is just a translator
     */

    //We need to catch apb flash hit on apb, 
    //let the xip logic control the spi_top_apb logic
    //send apb packet
    //when done exit xip mode

    //the whole logic relies on the packet send to mspi
      
    //The arbiter: decide whose packet can go into mspi:
      //the two sources:
        //XIP logic;
        //standard in;
      //when in XIP mode: we also need to take over the utter apb reply: do not send ready!
      //when in common mode: let the spi_top_apb slaves to control
    //the standard in
    


    //Here XIP works as a blocking function
    //inout: APBSPI ready for begin, valid for done
    class XIP {
      val targetAddr = in.paddr
      val flashData  = Wire(UInt(32.W))
      class XIPOp extends Bundle/* what is Bundle? why we need Bundle for XIPOp to be Wire?*/{
        val addr = UInt(32.W)
        val data = UInt(32.W)
        val write = Bool()
      }
      object XIPOp {
        def apply(addr: UInt,/* what is UInt? An apply method? what is UInt(32.W)? */ data: UInt, write: Bool = true.B) = {
          val res = Wire(new XIPOp)
          res.addr := addr
          res.data := data
          res.write := write 
          res
        }
      }
      val spiBase = 0x30000000
      val opSeq = VecInit(Seq(
          XIPOp((spiBase + 0x00).U, 0x00.U), 
          XIPOp((spiBase + 0x04).U, (0x03.U << 24) | targetAddr  ),
          XIPOp((spiBase + 0x14).U, 0.U),
          XIPOp((spiBase + 0x18).U, 1.U),
          XIPOp((spiBase + 0x10).U, 0x2540.U),
          XIPOp((spiBase + 0x10).U, 0.U, false.B),             
          XIPOp((spiBase + 0x00).U, 0.U, false.B)   
      ))

      // APBHelper: manage apb send.
      object APBHelper {
        def driveAPB(req: DecoupledIO[XIPOp]/* what is this? */, apb: APBBundle): Bool = {
          val sIdle :: sSetup :: sAccess :: Nil = Enum(3)// Enum in Chisel?
          val state = RegInit(sIdle)
          val r = RegEnable (req.bits, req.fire)
          val activeReq = Mux(state === sIdle, req.bits, r)
          switch (state) {
            is (sIdle) {
              when (req.valid) {
                state := sSetup
              }
            }
            is (sSetup) { //used to get psel on
              state := sAccess
            }
            is (sAccess) {
              when (apb.pready){
                state := Mux(req.valid, sSetup, sIdle)
              }
            }
          }
          apb.psel := (state === sSetup) || (state === sAccess) 
          apb.penable := (state === sAccess )
          apb.paddr := activeReq.addr
          apb.pwdata := activeReq.data
          apb.pwrite := activeReq.write
          val done = state === sAccess && apb.pready
          req.ready := done
          done
        }
      }
      
      val upc = RegInit(0.U(3.W))
      val busy = RegInit(false.B)
      val flashData = RegInit(0.U(32.W))
      val inst = DecoupledIO(opSeq(upc))
      val step = WireDefault(false.B)
      inst.valid := step
      val stepDone = APBHelper.driveAPB(inst, mspi.io.in)
      val seqDone  = WireDefault(false.B)
      def execute (start: Bool, apb: APBBundle): (Bool, UInt) = {
        when (!busy && start) {
          busy := true.B
          upc := 0.U
        }
        when (busy && stepDone) {
          when (upc < 6.U) {
            upc := upc + 1.U
          } .otherwise {
            seqDone := true.B
            fl@@
            upc := 0.U
            busy := false.B
          }
        }


      }




      when (io.req.valid && !busy) {
        busy := true.B
        step := true.B
      }
      
      //val step = inst.valid//I want to git inst.valid another name, is this Valid? NO!!!

      
  

      
      when (busy) {
        step := true.B
        when (done) {
          when (upc < 7.U) {
            upc := upc + 1.U
          } .otherwise {
            upc := 0.U
            busy := false.B
          }
        }
      }


    }



    val xip = new XIP
    val xipDone = xip.io.resp.valid
    val xipStart = WireDefault(false.B)
    xip.io.req.valid := xipStart
    



    val flashHit = in.psel && in.penable && (in.paddr >= 0x30000000.U && in.paddr < 0x40000000.U)
    val sNormal :: sXIP :: Nil = Enum(2)
    val state = RegInit(sNormal)

    switch(state) {
      is (sNormal) {
        when (flashHit) {
          state := sXIP
        }
      }
      is (sXIP) {
        when (xip.io.resp.valid) {
          state := sNormal
        }
      }
    }



    mspi.io.clock := clock
    mspi.io.reset := reset
    mspi.io.in <> in




    spi_bundle <> mspi.io.spi

  }
}

```


presentation compiler configuration:
Scala version: 2.13.14
Classpath:
<WORKSPACE>/.bloop/out/ysyxsoc/bloop-bsp-clients-classes/classes-Metals-XdamSpcfTkaiLmdsY8dc9A== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.11.1/semanticdb-javac-0.11.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/chisel_2.13/7.0.0-M2/chisel_2.13-7.0.0-M2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.14/scala-library-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/mainargs_2.13/0.5.4/mainargs_2.13-0.5.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-jackson_2.13/4.0.6/json4s-jackson_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.14/scala-reflect-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/sourcecode_2.13/0.3.1/sourcecode_2.13-0.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.13/4.1.0/scopt_2.13-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native_2.13/4.0.7/json4s-native_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-text/1.12.0/commons-text-1.12.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/github/alexarchambault/data-class_2.13/0.2.6/data-class_2.13-0.2.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/os-lib_2.13/0.10.0/os-lib_2.13-0.10.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-parallel-collections_2.13/1.0.4/scala-parallel-collections_2.13-1.0.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle_2.13/3.3.0/upickle_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/firtool-resolver_2.13/2.0.0/firtool-resolver_2.13-2.0.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-collection-compat_2.13/2.11.0/scala-collection-compat_2.13-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-core_2.13/4.0.7/json4s-core_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-jackson-core_2.13/4.0.6/json4s-jackson-core_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native-core_2.13/4.0.7/json4s-native-core_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/geny_2.13/1.1.0/geny_2.13-1.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/ujson_2.13/3.3.0/ujson_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upack_2.13/3.3.0/upack_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-implicits_2.13/3.3.0/upickle-implicits_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.13/2.2.0/scala-xml_2.13-2.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-ast_2.13/4.0.7/json4s-ast_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-scalap_2.13/4.0.7/json4s-scalap_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/thoughtworks/paranamer/paranamer/2.8/paranamer-2.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.12.7/jackson-databind-2.12.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-core_2.13/3.3.0/upickle-core_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.12.7/jackson-annotations-2.12.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.12.7/jackson-core-2.12.7.jar [exists ], <WORKSPACE>/rocket-chip/dependencies/cde/cde/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.cde/bloop-bsp-clients-classes/classes-Metals-XdamSpcfTkaiLmdsY8dc9A== [exists ], <WORKSPACE>/rocket-chip/dependencies/diplomacy/diplomacy/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.diplomacy/bloop-bsp-clients-classes/classes-Metals-XdamSpcfTkaiLmdsY8dc9A== [exists ], <WORKSPACE>/rocket-chip/dependencies/hardfloat/hardfloat/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.hardfloat/bloop-bsp-clients-classes/classes-Metals-XdamSpcfTkaiLmdsY8dc9A== [exists ], <WORKSPACE>/rocket-chip/macros/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.macros/bloop-bsp-clients-classes/classes-Metals-XdamSpcfTkaiLmdsY8dc9A== [exists ], <WORKSPACE>/rocket-chip/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip/bloop-bsp-clients-classes/classes-Metals-XdamSpcfTkaiLmdsY8dc9A== [exists ], <WORKSPACE>/compile-resources [missing ]
Options:
-language:reflectiveCalls -Ymacro-annotations -Ytasty-reader -Yrangepos -Xplugin-require:semanticdb




#### Error stacktrace:

```
scala.collection.mutable.ArrayBuffer.apply(ArrayBuffer.scala:102)
	scala.reflect.internal.Types$Type.findMemberInternal$1(Types.scala:1030)
	scala.reflect.internal.Types$Type.findMember(Types.scala:1035)
	scala.reflect.internal.Types$Type.memberBasedOnName(Types.scala:661)
	scala.reflect.internal.Types$Type.nonLocalMember(Types.scala:652)
	scala.tools.nsc.typechecker.Contexts$ImportInfo.importedSelectedSymbol(Contexts.scala:1944)
	scala.tools.nsc.typechecker.Contexts$SymbolLookup.apply(Contexts.scala:1530)
	scala.tools.nsc.typechecker.Contexts$Context.lookupSymbol(Contexts.scala:1282)
	scala.tools.nsc.typechecker.Typers$Typer.typedIdent$2(Typers.scala:5663)
	scala.tools.nsc.typechecker.Typers$Typer.typedIdentOrWildcard$1(Typers.scala:5732)
	scala.tools.nsc.typechecker.Typers$Typer.typed1(Typers.scala:6203)
	scala.tools.nsc.typechecker.Typers$Typer.typed(Typers.scala:6261)
	scala.tools.nsc.typechecker.Typers$Typer.typedTypeConstructor(Typers.scala:6460)
	scala.tools.nsc.typechecker.Typers$Typer.typedTypeConstructor(Typers.scala:6477)
	scala.tools.nsc.typechecker.Typers$Typer.typedParentType(Typers.scala:1666)
	scala.tools.nsc.typechecker.Typers$Typer.typedParentTypes(Typers.scala:1832)
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

java.lang.IndexOutOfBoundsException: -1 is out of bounds (min 0, max 2)