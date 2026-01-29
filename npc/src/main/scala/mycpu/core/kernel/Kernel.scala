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

  /**
   * 挂载一个物理驱动到内核
   */
  def mount(driver: PhysicalDriver): Unit = {
    drivers(driver.meta.name) = driver
    clients(driver.meta.name) = ArrayBuffer[ClientChannel]()
  }

  def hasDriver(name: String): Boolean = drivers.contains(name)

  /**
   * 为进程创建一个访问特定驱动的虚拟连接
   */
  def createConnection(driverName: String): VirtualResourceHandle = {
    if (!drivers.contains(driverName)) throw new Exception(s"[Kernel] Unknown driver: $driverName")
    val drv = drivers(driverName)
    
    // 创建一个全局宽度的通道 (64位)，确保数据不被截断
    val channel = Wire(new ClientChannel(32, KERNEL_DATA_WIDTH))
    
    // 初始化默认值，防止 Chisel 报错
    channel.req.valid := false.B
    channel.req.addr  := 0.U
    channel.req.data  := 0.U
    channel.req.size  := 0.U
    channel.req.wen   := false.B
    channel.respData  := 0.U
    channel.ready     := false.B
    channel.error     := Errno.ESUCCESS
    
    clients(driverName) += channel
    new VirtualResourceHandle(drv.meta, channel)
  }

  /**
   * 启动内核：为每个驱动生成对应的资源仲裁逻辑
   */
  def boot(): Unit = {
    drivers.foreach { case (name, drv) =>
      val driverClients = clients(name)
      if (driverClients.nonEmpty) generateResourceManager(drv, driverClients)
    }
  }

  /**
   * 生成资源管理器：处理多客户端竞争同一物理驱动的情况
   */
  private def generateResourceManager(drv: PhysicalDriver, channels: ArrayBuffer[ClientChannel]): Unit = {
    val meta = drv.meta
    
    // 为该驱动创建一个独立的内核代理逻辑块
    val logic = new HardwareLogic(s"kArbiter_${meta.name}", debugEnable = false)
    
    // 物理层初始化
    drv.setup(logic)

    logic.run {
      // 1. 默认所有通道不响应
      for (ch <- channels) {
        ch.respData := 0.U
        ch.ready    := false.B
        ch.error    := Errno.ESUCCESS
      }

      // 2. 状态寄存器
      val rBusy = RegInit(false.B)
      val wBusy = RegInit(false.B)
      val rIdx  = Reg(UInt(log2Ceil(channels.length max 1).W))
      val wIdx  = Reg(UInt(log2Ceil(channels.length max 1).W))

      // 【关键修复】请求参数锁存器
      // 必须锁存地址和大小，防止 AXI 事务进行中时，由于客户端 Step 切换导致地址抖动
      val activeRAddr = Reg(UInt(32.W))
      val activeRSize = Reg(UInt(2.W))
      val activeWAddr = Reg(UInt(32.W))
      val activeWData = Reg(UInt(KERNEL_DATA_WIDTH.W))
      val activeWSize = Reg(UInt(2.W))

      // 3. 收集并仲裁读写请求
      val readReqSignals = channels.map(ch => ch.req.valid && !ch.req.wen && (meta.readTiming == DriverTiming.Sequential).B)
      val writeReqSignals = channels.map(ch => ch.req.valid && ch.req.wen)
      
      val readReqs  = VecInit(readReqSignals.toSeq).asUInt
      val writeReqs = VecInit(writeReqSignals.toSeq).asUInt
      
      val nextRIdx = PriorityEncoder(readReqs)
      val nextWIdx = PriorityEncoder(writeReqs)

      // --- 读事务逻辑 (Sequential) ---
      when(!rBusy) {
        when(readReqs.orR) {
          rBusy := true.B
          rIdx  := nextRIdx
          // 锁存当前请求的参数
          activeRAddr := VecInit(channels.map(_.req.addr).toSeq)(nextRIdx)
          activeRSize := VecInit(channels.map(_.req.size).toSeq)(nextRIdx)
        }
      } .otherwise {
        // 调用物理驱动的顺序读接口
        val res = drv.seqRead(activeRAddr, activeRSize)
        val data: UInt = res._1
        val err:  UInt = res._2
        val done: Bool = res._3

        // 将结果反馈给选中的通道
        for ((ch, i) <- channels.zipWithIndex) {
          when(rIdx === i.U) {
            ch.respData := data 
            ch.ready    := done
            ch.error    := err
          }
        }
        // 只有当物理驱动报告完成时，才释放 busy 锁
        when(done) { rBusy := false.B }
      }

      // --- 写事务逻辑 (Sequential) ---
      when(!wBusy) {
        when(writeReqs.orR) {
          wBusy := true.B
          wIdx  := nextWIdx
          // 锁存写操作参数
          activeWAddr := VecInit(channels.map(_.req.addr).toSeq)(nextWIdx)
          activeWData := VecInit(channels.map(_.req.data).toSeq)(nextWIdx)
          activeWSize := VecInit(channels.map(_.req.size).toSeq)(nextWIdx)
        }
      } .otherwise {
        // 调用物理驱动的顺序写接口
        val res = drv.seqWrite(activeWAddr, activeWData, activeWSize)
        val err:  UInt = res._1
        val done: Bool = res._2

        for ((ch, i) <- channels.zipWithIndex) {
          when(wIdx === i.U) {
            ch.ready := done
            ch.error := err
          }
        }
        when(done) { wBusy := false.B }
      }
      
      // --- 组合读逻辑 (Bypass/Combinational) ---
      // 适用于 RegFile, PC 等不需要握手的设备
      if (meta.readTiming == DriverTiming.Combinational) {
        for (ch <- channels) {
          // 组合设备不参与仲裁，只要 valid 就立即响应
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