package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.utils._
import mycpu.common._
import scala.collection.mutable.{ArrayBuffer, HashMap}

class Kernel {
  private val drivers = new HashMap[String, PhysicalDriver]()
  private val clients = new HashMap[String, ArrayBuffer[ClientChannel]]()

  def mount(driver: PhysicalDriver): Unit = {
    drivers(driver.meta.name) = driver
    clients(driver.meta.name) = ArrayBuffer[ClientChannel]()
  }

  def hasDriver(name: String): Boolean = drivers.contains(name)

  def createConnection(driverName: String): VirtualResourceHandle = {
    if (!drivers.contains(driverName)) throw new Exception(s"Unknown driver: $driverName")
    val drv = drivers(driverName)
    
    // 使用全局定义的宽位宽 (64位)
    val channel = Wire(new ClientChannel(32, KERNEL_DATA_WIDTH))
    
    channel.req.valid := false.B
    channel.req.addr  := 0.U
    channel.req.data  := 0.U
    channel.req.size  := 0.U
    channel.req.wen   := false.B
    channel.respData := 0.U
    channel.ready    := false.B
    channel.error    := 0.U
    clients(driverName) += channel
    new VirtualResourceHandle(drv.meta, channel)
  }

  def boot(): Unit = {
    drivers.foreach { case (name, drv) =>
      val driverClients = clients(name)
      if (driverClients.nonEmpty) generateResourceManager(drv, driverClients)
    }
  }

  private def generateResourceManager(drv: PhysicalDriver, channels: ArrayBuffer[ClientChannel]): Unit = {
    val meta = drv.meta
    
    val logic = new HardwareLogic(s"kThread_${meta.name}", debugEnable = true)
    
    drv.setup(logic)

    logic.run {
      // [关键修复] 为所有通道赋予默认值，防止由 latch 推断导致的组合逻辑环路
      for (ch <- channels) {
        ch.respData := 0.U
        ch.ready    := false.B
        ch.error    := Errno.ESUCCESS
      }

      val rBusy = RegInit(false.B)
      val wBusy = RegInit(false.B)
      val rIdx  = Reg(UInt(log2Ceil(channels.length max 1).W))
      val wIdx  = Reg(UInt(log2Ceil(channels.length max 1).W))

      val readReqs  = channels.map(ch => ch.req.valid && !ch.req.wen && (meta.readTiming == DriverTiming.Sequential).B).toSeq
      val writeReqs = channels.map(ch => ch.req.valid && ch.req.wen).toSeq
      
      val nextRIdx = PriorityEncoder(readReqs)
      val nextWIdx = PriorityEncoder(writeReqs)

      // --- 读事务逻辑 ---
      when(!rBusy) {
        when(VecInit(readReqs).asUInt.orR) {
          rBusy := true.B
          rIdx  := nextRIdx
        }
      } .otherwise {
        val (data, err, done) = drv.seqRead(
          VecInit(channels.map(_.req.addr).toSeq)(rIdx),
          VecInit(channels.map(_.req.size).toSeq)(rIdx)
        )
        for ((ch, i) <- channels.zipWithIndex) {
          when(rIdx === i.U) {
            ch.respData := data 
            ch.ready    := done
            ch.error    := err
          }
        }
        when(done) { rBusy := false.B }
      }

      // --- 写事务逻辑 ---
      when(!wBusy) {
        when(VecInit(writeReqs).asUInt.orR) {
          wBusy := true.B
          wIdx  := nextWIdx
        }
      } .otherwise {
        val (err, done) = drv.seqWrite(
          VecInit(channels.map(_.req.addr).toSeq)(wIdx),
          VecInit(channels.map(_.req.data).toSeq)(wIdx), 
          VecInit(channels.map(_.req.size).toSeq)(wIdx)
        )
        for ((ch, i) <- channels.zipWithIndex) {
          when(wIdx === i.U) {
            ch.ready := done
            ch.error := err
          }
        }
        when(done) { wBusy := false.B }
      }
      
      // 组合读逻辑 (Bypass)
      if (meta.readTiming == DriverTiming.Combinational) {
        for (ch <- channels) {
          when(ch.req.valid && !ch.req.wen) {
            ch.respData := drv.combRead(ch.req.addr, ch.req.size)
            ch.ready    := true.B
            ch.error    := Errno.ESUCCESS
          }
        }
      }
    }
  }
}