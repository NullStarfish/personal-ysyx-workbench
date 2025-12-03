package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu.common._

// ==============================================================================
// 1. AXI4 常量定义
// ==============================================================================
object AXI4Parameters {
  val BURST_FIXED = 0.U(2.W)
  val BURST_INCR  = 1.U(2.W)
  val BURST_WRAP  = 2.U(2.W)

  val RESP_OKAY   = 0.U(2.W)
  val RESP_EXOKAY = 1.U(2.W)
  val RESP_SLVERR = 2.U(2.W)
  val RESP_DECERR = 3.U(2.W)

  val CACHE_DEVICE_NOBUF = 0.U(4.W) 
  val PROT_PRIVILEGED    = 1.U(3.W) 
}

// ==============================================================================
// 2. AXI4 通道 Payload 定义
// ==============================================================================
class AXI4BundleA(val idWidth: Int, val addrWidth: Int) extends Bundle {
  val id    = UInt(idWidth.W)
  val addr  = UInt(addrWidth.W)
  val len   = UInt(8.W)
  val size  = UInt(3.W)
  val burst = UInt(2.W)
  val lock  = Bool()
  val cache = UInt(4.W)
  val prot  = UInt(3.W)
  val qos   = UInt(4.W)
}

class AXI4BundleW(val dataWidth: Int) extends Bundle {
  val data = UInt(dataWidth.W)
  val strb = UInt((dataWidth/8).W)
  val last = Bool()
}

class AXI4BundleB(val idWidth: Int) extends Bundle {
  val id   = UInt(idWidth.W)
  val resp = UInt(2.W)
}

class AXI4BundleR(val idWidth: Int, val dataWidth: Int) extends Bundle {
  val id   = UInt(idWidth.W)
  val data = UInt(dataWidth.W)
  val resp = UInt(2.W)
  val last = Bool()
}

// ==============================================================================
// 3. 基础 Bundle 定义 (含 Helper 函数)
// ==============================================================================

class AXI4Bundle(val idWidth: Int, val addrWidth: Int, val dataWidth: Int) extends Bundle {
  // AXI4 标准方向: Master -> Slave
  val aw = Decoupled(new AXI4BundleA(idWidth, addrWidth))
  val w  = Decoupled(new AXI4BundleW(dataWidth))
  val b  = Flipped(Decoupled(new AXI4BundleB(idWidth)))
  val ar = Decoupled(new AXI4BundleA(idWidth, addrWidth))
  val r  = Flipped(Decoupled(new AXI4BundleR(idWidth, dataWidth)))

  // ------------------------------------------------------------------------
  // Helper: 初始化 Master 端口输出 (用于 io.out 或非 Flipped 的 Bundle)
  // 作用: 将 Valid 置低，Ready 置低，Payload 设为 DontCare
  // ------------------------------------------------------------------------
  def setAsMasterInit(): Unit = {
    // Master 发送请求 (Valid = 0)
    aw.valid := false.B; aw.bits := DontCare
    w.valid  := false.B; w.bits  := DontCare
    ar.valid := false.B; ar.bits := DontCare
    
    // Master 接收响应 (Ready = 0)
    b.ready  := false.B
    r.ready  := false.B
  }

  // ------------------------------------------------------------------------
  // Helper: 初始化 Slave 端口输出 (用于 io.in 或 Flipped 的 Bundle)
  // 作用: 将 Ready 置低，Valid 置低 (响应通道)
  // ------------------------------------------------------------------------
  def setAsSlaveInit(): Unit = {
    // Slave 接收请求 (Ready = 0)
    aw.ready := false.B
    w.ready  := false.B
    ar.ready := false.B

    // Slave 发送响应 (Valid = 0)
    b.valid  := false.B; b.bits := DontCare
    r.valid  := false.B; r.bits := DontCare
  }
}

// AXI4-Lite 继承自 AXI4Bundle，因此自动拥有上述 helper 函数
class AXI4LiteBundle(addrWidth: Int = XLEN, dataWidth: Int = XLEN) 
  extends AXI4Bundle(idWidth = 1, addrWidth = addrWidth, dataWidth = dataWidth)

// ==============================================================================
// 4. 拆分用 Bundle & Splitter
// ==============================================================================
class AXI4ReadOnly(val idWidth: Int, val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val ar = Decoupled(new AXI4BundleA(idWidth, addrWidth))
  val r  = Flipped(Decoupled(new AXI4BundleR(idWidth, dataWidth)))
}

class AXI4WriteOnly(val idWidth: Int, val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val aw = Decoupled(new AXI4BundleA(idWidth, addrWidth))
  val w  = Decoupled(new AXI4BundleW(dataWidth))
  val b  = Flipped(Decoupled(new AXI4BundleB(idWidth)))
}

object AXI4Split {
  def unapply(axi: AXI4Bundle): Option[(AXI4ReadOnly, AXI4WriteOnly)] = {
    val r = Wire(new AXI4ReadOnly(axi.idWidth, axi.addrWidth, axi.dataWidth))
    val w = Wire(new AXI4WriteOnly(axi.idWidth, axi.addrWidth, axi.dataWidth))
    axi.ar <> r.ar
    axi.r  <> r.r
    axi.aw <> w.aw
    axi.w  <> w.w
    axi.b  <> w.b
    Some((r, w))
  }
}