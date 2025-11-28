package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu.common.XLEN


//======================================================
//注意： addrWidth为byte寻址，dataWidth为bit位宽
//=====================================================






// ==============================================================================
// 1. 基础 Payload 定义
// ==============================================================================
class AXIAddress(width: Int) extends Bundle {
  val addr = UInt(width.W)
  val prot = UInt(3.W)
}
class AXIWriteData(width: Int) extends Bundle {
  val data = UInt(width.W)
  val strb = UInt((width/8).W)
}
class AXIResp extends Bundle {
  val resp = UInt(2.W)
}
class AXIReadData(width: Int) extends Bundle {
  val data = UInt(width.W)
  val resp = UInt(2.W)
}

// ==============================================================================
// 2. Trait 定义 (实现接口拆分的关键)
// ==============================================================================

// 特质：只包含读通道
trait HasAXI4LiteRead { this: Bundle =>
  val addrWidth: Int
  val dataWidth: Int
  
  val ar = Decoupled(new AXIAddress(addrWidth))
  val r  = Flipped(Decoupled(new AXIReadData(dataWidth)))
}

// 特质：只包含写通道
trait HasAXI4LiteWrite { this: Bundle =>
  val addrWidth: Int
  val dataWidth: Int

  val aw = Decoupled(new AXIAddress(addrWidth))
  val w  = Decoupled(new AXIWriteData(dataWidth))
  val b  = Flipped(Decoupled(new AXIResp()))
}

// ==============================================================================
// 3. 具体 Bundle 类
// ==============================================================================

// [New] 纯读接口 Bundle (用于 ReadBridge)
class AXI4LiteReadBundle(val addrWidth: Int = XLEN, val dataWidth: Int = XLEN) 
  extends Bundle with HasAXI4LiteRead

// [New] 纯写接口 Bundle (用于 WriteBridge)
class AXI4LiteWriteBundle(val addrWidth: Int = XLEN, val dataWidth: Int = XLEN) 
  extends Bundle with HasAXI4LiteWrite

// [Legacy/Top] 完整接口 Bundle (用于 Fetch/LSU IO 和 Top IO)
class AXI4LiteBundle(val addrWidth: Int = XLEN, val dataWidth: Int = XLEN) 
  extends Bundle with HasAXI4LiteRead with HasAXI4LiteWrite {
  
  // 辅助方法：初始化 Master 输出 (防止未驱动)
  def setAsMaster(): Unit = {
    ar.valid := false.B; ar.bits := DontCare
    aw.valid := false.B; aw.bits := DontCare
    w.valid  := false.B; w.bits  := DontCare
    r.ready  := false.B
    b.ready  := false.B
  }

  def setAsSlave(): Unit = {
    ar.ready := false.B;  
    aw.ready := false.B;
    w.ready  := false.B;
    r.valid  := false.B; r.bits := DontCare
    b.valid  := false.B; b.bits := DontCare
  }
}

// ==============================================================================
// 4. 魔法提取器 (The Magic Splitter)
// ==============================================================================
object AXI4Split {
  // 支持语法: val AXI4Split(rBus, wBus) = io.axi
  def unapply(axi: AXI4LiteBundle): Option[(AXI4LiteReadBundle, AXI4LiteWriteBundle)] = {
    // 创建中间 Wire
    val r = Wire(new AXI4LiteReadBundle(axi.addrWidth, axi.dataWidth))
    val w = Wire(new AXI4LiteWriteBundle(axi.addrWidth, axi.dataWidth))

    // 建立双向连接
    // 当 axi 是 Master Port (Output) 时，axi 驱动 Wire
    // 当 Wire 连接到 Submodule (Bridge) 时，Bridge 驱动 Wire，Wire 驱动 axi
    axi.ar <> r.ar
    axi.r  <> r.r
    
    axi.aw <> w.aw
    axi.w  <> w.w
    axi.b  <> w.b
    
    Some((r, w))
  }
}