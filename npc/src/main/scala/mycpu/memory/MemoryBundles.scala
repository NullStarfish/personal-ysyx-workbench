package mycpu.memory

import chisel3._
import chisel3.util._
import mycpu.common._

class FetchResp extends Bundle {
  val inst = UInt(32.W)
  val hit = Bool()
}

class MemA extends Bundle {
  val addr = XLenU
  val size = UInt(3.W)
  val len = UInt(8.W)
  val write = Bool()
  val id = UInt(4.W)
}

class MemW extends Bundle {
  val data = XLenU
  val strb = UInt(4.W)
  val last = Bool()
}

class MemR extends Bundle {
  val data = XLenU
  val resp = UInt(2.W)
  val last = Bool()
  val id = UInt(4.W)
}

class MemB extends Bundle {
  val resp = UInt(2.W)
  val id = UInt(4.W)
}

class MemReadIO extends Bundle {
  val a = Decoupled(new MemA)
  val r = Flipped(Decoupled(new MemR))
}

class MemIO extends Bundle {
  val a = Decoupled(new MemA)
  val w = Decoupled(new MemW)
  val r = Flipped(Decoupled(new MemR))
  val b = Flipped(Decoupled(new MemB))
}
