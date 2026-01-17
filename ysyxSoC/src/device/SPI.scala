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




    val sNormal :: sXIP :: Nil = Enum(2)
    val state = RegInit(sNormal)
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
