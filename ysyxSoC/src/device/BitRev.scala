package ysyx

import chisel3._
import chisel3.util._

class bitrev extends BlackBox {
  val io = IO(Flipped(new SPIIO(1)))
}

class bitrevChisel extends RawModule {
  val io = IO(Flipped(new SPIIO(1)))
  val rst = io.ss(0).asAsyncReset

  // 1. 下降沿采样：此时 MOSI 最稳定
  val rx = withClock((!io.sck).asClock) {
    val r = Reg(UInt(8.W))
    r := Cat(r(6, 0), io.mosi) // Push
    r
  }

  // 2. 上升沿接力：弹栈逻辑
  val (cnt, tx) = withClockAndReset(io.sck.asClock, rst) {
    val c = RegInit(0.U(5.W))
    val t = RegInit(0.U(8.W))
    c := c + 1.U

    // 关键修正：
    // 当 c 为 8 时（即第 9 个上升沿），rx 已经完整拿到了 8 位数据
    when (c === 8.U) {
      t := rx           // 加载完整的 8 位
    } .elsewhen (c >= 9.U) {
      t := t >> 1.U     // 从第 10 个上升沿才开始右移
    }
    (c, t)
  }

  // 3. 输出逻辑：
  // 此时 cnt 在第 9 拍期间是 9。只要 cnt >= 9 且未结束，就输出 tx(0)
  // 当 ss 为 1 时输出 true.B (SPI 默认高电平闲置)
  io.miso := Mux(io.ss(0), true.B, Mux(cnt >= 9.U && cnt < 17.U, tx(0), 0.B))
}