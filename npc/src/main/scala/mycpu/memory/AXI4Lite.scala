package mycpu.memory

import chisel3._
import chisel3.util._
import mycpu.common._

// 定义 AXI4-Lite Bundle
class AXI4LiteBundle extends Bundle {
  // Write Address Channel
  val aw = Decoupled(new Bundle {
    val addr = UInt(XLEN.W)
    val prot = UInt(3.W) // Optional
  })
  
  // Write Data Channel
  val w = Decoupled(new Bundle {
    val data = UInt(XLEN.W)
    val strb = UInt((XLEN/8).W)
  })

  // Write Response Channel
  val b = Flipped(Decoupled(new Bundle {
    val resp = UInt(2.W)
  }))

  // Read Address Channel
  val ar = Decoupled(new Bundle {
    val addr = UInt(XLEN.W)
    val prot = UInt(3.W)
  })

  // Read Data Channel
  val r = Flipped(Decoupled(new Bundle {
    val data = UInt(XLEN.W)
    val resp = UInt(2.W)
  }))
}

// Master 视角 IO (CPU 端)
class AXI4LiteMasterIO extends AXI4LiteBundle

// Slave 视角 IO (SRAM 端)
class AXI4LiteSlaveIO extends Bundle {
  // 使用 Flipped 翻转方向：Slave 输入 addr，输出 data
  val slave = Flipped(new AXI4LiteBundle)
}