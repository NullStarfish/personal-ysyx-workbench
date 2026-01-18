error id: D7157D4D71A7EF2FD368C3694FF52332
file://<WORKSPACE>/src/device/SPI.scala
### java.util.NoSuchElementException: head of empty String

occurred in the presentation compiler.



action parameters:
offset: 2807
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
    val sNormal :: sXIP :: Nil = Enum(2)
    val state = RegInit(sNormal)


       

    abstract class BlockingProcedure(parentState: UInt) {
      val busy = RegInit(false.B)

      protected def logic(fire: Bool) : (Bool, UInt)


      def run(returnState: UInt): UInt = {

        val isCallerActive = true.B
        val fire = !busy && isCallerActive

        when (fire) {busy := true.B}

        val (done, data) = logic (fire)

        when (busy && done) {
          busy := false.B
          parentState := returnState
        }

        data

        
      }
    }
    class XIPProcedure(parentState: UInt) extends BlockingProcedure(parentState) {
      val spiBase = 0x10001000
      val upc = RegInit(0.U(3.W))
      val targetAddr = Reg(UInt(32.W))
      val dataReg = Reg(UInt(32.W))

      def Op(addr: UInt, wdata: UInt, write: Bool = true.B, strb: UInt = "hF".U) = {
        val res = Wire(new Bundle {
          val addr = UInt(32.W)
          val wdata = UInt(32.W)
          val write = Bool()
          val strb = UInt(4.W)
        })
        res.addr := addr
        res.wdata := wdata
        res.write := write
        res.strb := strb
        res
      }


      val opSeq = VecInit(
          XIPOp((spiBase + 0x00).U, 0x00.U), 
          XIPOp((spiBase + 0x04).U, (0x03.U << 24) | (targetAddr & 0x00FFFFFF.U)),
          XIPOp((spiBase + 0x14).U, 0.U),
          XIPOp((spiBase + 0x18).U, 1.U),
          XIPOp((spiBase + 0x10).U, 0x2540.U),
          XIPOp((spiBase + 0x10).U, 0.U, false.B),             
          @@XIPOp((spiBase + 0x00).U, 0.U, false.B)   
        Op
      )


    }


    //Here XIP works as a blocking function
    //inout: APBSPI ready for begin, valid for done
    class XIP {
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
      val spiBase = 0x10001000
      
      // APBHelper: manage apb send.
      object APBHelper {
        def driveAPB(req: DecoupledIO[XIPOp]/* what is this? */, apb: APBBundle): Bool = {
          val sIdle :: sSetup :: sAccess :: Nil = Enum(3)// Enum in Chisel?
          val state = RegInit(sIdle)
          val activeReq = req.bits
          switch (state) {
            is (sIdle) {
              when (req.valid) {
                state := sSetup
              }
            }
            is (sSetup) { // used to get psel
              state := sAccess
            }
            is (sAccess) { //the Setup and Access realize continuly write & read
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
          apb.pstrb := 0xF.U
          val done = (state === sAccess) && (apb.pready) // A PULSE
          req.ready := done
          done
        }
      }
      
      val upc = RegInit(0.U(3.W))
      val busy = RegInit(false.B)
      val targetAddr = Reg(UInt(32.W))
        

      val dataReg  = Reg(UInt(32.W))
      def execute (start: Bool, apb: APBBundle): (Bool, UInt) = {
        /* start : the functions's start sign
          apb: the XIP proxy the in APB
          Bool: seqDone
          UInt: flash out */
        
        // IN.paddr is OUTSIDE the XIP!!!
        when(start) {
          targetAddr := in.paddr
        }       
        val opSeq = VecInit(Seq(
          XIPOp((spiBase + 0x00).U, 0x00.U), 
          XIPOp((spiBase + 0x04).U, (0x03.U << 24) | (targetAddr & 0x00FFFFFF.U)),
          XIPOp((spiBase + 0x14).U, 0.U),
          XIPOp((spiBase + 0x18).U, 1.U),
          XIPOp((spiBase + 0x10).U, 0x2540.U),
          XIPOp((spiBase + 0x10).U, 0.U, false.B),             
          XIPOp((spiBase + 0x00).U, 0.U, false.B)   
        ))

        //IT'S WRONG !!!j 
        //val inst = Wire(Decoupled(opSeq(upc)))
        val inst = Wire(Decoupled(new XIPOp))
        inst.bits := opSeq(upc)
        inst.valid := busy


        val stepDone = APBHelper.driveAPB(inst, apb)
        val seqDone  = WireDefault(false.B)

        when (!busy && start) {
          busy := true.B
          inst.valid := true.B
          upc := 0.U
        }
        when (busy && stepDone) {
          when (upc === 5.U) {
            val isFinished = (apb.prdata & 0x100.U) === 0.U
            when (isFinished) {upc := upc + 1.U}
          } .elsewhen (upc < 6.U) {
            upc := upc + 1.U
          } .otherwise {
            seqDone := true.B
            upc := 0.U
            busy := false.B

            dataReg := apb.prdata
          }
        }
        /* cause we are driving outter signals,
            INCOMPLETELY!!!  */

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


presentation compiler configuration:
Scala version: 2.13.14
Classpath:
<WORKSPACE>/.bloop/out/ysyxsoc/bloop-bsp-clients-classes/classes-Metals-JSuADs1URp6k-866XTQ5BA== [exists ], <HOME>/.cache/bloop/semanticdb/com.sourcegraph.semanticdb-javac.0.11.1/semanticdb-javac-0.11.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/chisel_2.13/7.0.0-M2/chisel_2.13-7.0.0-M2.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.13.14/scala-library-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/mainargs_2.13/0.5.4/mainargs_2.13-0.5.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-jackson_2.13/4.0.6/json4s-jackson_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-reflect/2.13.14/scala-reflect-2.13.14.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/sourcecode_2.13/0.3.1/sourcecode_2.13-0.3.1.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/github/scopt/scopt_2.13/4.1.0/scopt_2.13-4.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native_2.13/4.0.7/json4s-native_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-text/1.12.0/commons-text-1.12.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/io/github/alexarchambault/data-class_2.13/0.2.6/data-class_2.13-0.2.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/os-lib_2.13/0.10.0/os-lib_2.13-0.10.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-parallel-collections_2.13/1.0.4/scala-parallel-collections_2.13-1.0.4.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle_2.13/3.3.0/upickle_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/chipsalliance/firtool-resolver_2.13/2.0.0/firtool-resolver_2.13-2.0.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-collection-compat_2.13/2.11.0/scala-collection-compat_2.13-2.11.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-core_2.13/4.0.7/json4s-core_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-jackson-core_2.13/4.0.6/json4s-jackson-core_2.13-4.0.6.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-native-core_2.13/4.0.7/json4s-native-core_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/geny_2.13/1.1.0/geny_2.13-1.1.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/ujson_2.13/3.3.0/ujson_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upack_2.13/3.3.0/upack_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-implicits_2.13/3.3.0/upickle-implicits_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/modules/scala-xml_2.13/2.2.0/scala-xml_2.13-2.2.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-ast_2.13/4.0.7/json4s-ast_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/json4s/json4s-scalap_2.13/4.0.7/json4s-scalap_2.13-4.0.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/thoughtworks/paranamer/paranamer/2.8/paranamer-2.8.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.12.7/jackson-databind-2.12.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/lihaoyi/upickle-core_2.13/3.3.0/upickle-core_2.13-3.3.0.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.12.7/jackson-annotations-2.12.7.jar [exists ], <HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.12.7/jackson-core-2.12.7.jar [exists ], <WORKSPACE>/rocket-chip/dependencies/cde/cde/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.cde/bloop-bsp-clients-classes/classes-Metals-JSuADs1URp6k-866XTQ5BA== [exists ], <WORKSPACE>/rocket-chip/dependencies/diplomacy/diplomacy/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.diplomacy/bloop-bsp-clients-classes/classes-Metals-JSuADs1URp6k-866XTQ5BA== [exists ], <WORKSPACE>/rocket-chip/dependencies/hardfloat/hardfloat/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.hardfloat/bloop-bsp-clients-classes/classes-Metals-JSuADs1URp6k-866XTQ5BA== [exists ], <WORKSPACE>/rocket-chip/macros/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip.macros/bloop-bsp-clients-classes/classes-Metals-JSuADs1URp6k-866XTQ5BA== [exists ], <WORKSPACE>/rocket-chip/compile-resources [missing ], <WORKSPACE>/.bloop/out/rocketchip/bloop-bsp-clients-classes/classes-Metals-JSuADs1URp6k-866XTQ5BA== [exists ], <WORKSPACE>/compile-resources [missing ]
Options:
-language:reflectiveCalls -Ymacro-annotations -Ytasty-reader -Yrangepos -Xplugin-require:semanticdb




#### Error stacktrace:

```
scala.collection.StringOps$.head$extension(StringOps.scala:1124)
	scala.meta.internal.metals.ClassfileComparator.compare(ClassfileComparator.scala:30)
	scala.meta.internal.metals.ClassfileComparator.compare(ClassfileComparator.scala:3)
	java.base/java.util.PriorityQueue.siftUpUsingComparator(PriorityQueue.java:660)
	java.base/java.util.PriorityQueue.siftUp(PriorityQueue.java:637)
	java.base/java.util.PriorityQueue.offer(PriorityQueue.java:330)
	java.base/java.util.PriorityQueue.add(PriorityQueue.java:311)
	scala.meta.internal.metals.ClasspathSearch.$anonfun$search$3(ClasspathSearch.scala:32)
	scala.meta.internal.metals.ClasspathSearch.$anonfun$search$3$adapted(ClasspathSearch.scala:26)
	scala.collection.IterableOnceOps.foreach(IterableOnce.scala:630)
	scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:628)
	scala.collection.AbstractIterator.foreach(Iterator.scala:1313)
	scala.meta.internal.metals.ClasspathSearch.search(ClasspathSearch.scala:26)
	scala.meta.internal.metals.WorkspaceSymbolProvider.search(WorkspaceSymbolProvider.scala:107)
	scala.meta.internal.metals.MetalsSymbolSearch.search$1(MetalsSymbolSearch.scala:114)
	scala.meta.internal.metals.MetalsSymbolSearch.search(MetalsSymbolSearch.scala:118)
	scala.meta.internal.pc.AutoImportsProvider.autoImports(AutoImportsProvider.scala:48)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$autoImports$1(ScalaPresentationCompiler.scala:399)
	scala.meta.internal.pc.CompilerAccess.withSharedCompiler(CompilerAccess.scala:148)
	scala.meta.internal.pc.CompilerAccess.$anonfun$withInterruptableCompiler$1(CompilerAccess.scala:92)
	scala.meta.internal.pc.CompilerAccess.$anonfun$onCompilerJobQueue$1(CompilerAccess.scala:209)
	scala.meta.internal.pc.CompilerJobQueue$Job.run(CompilerJobQueue.scala:152)
	java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)
	java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)
	java.base/java.lang.Thread.run(Thread.java:1583)
```
#### Short summary: 

java.util.NoSuchElementException: head of empty String