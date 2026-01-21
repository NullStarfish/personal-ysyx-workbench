package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.utils._
import chisel3.experimental.BundleLiterals._

class Fetch extends Module {
  val io = IO(new Bundle {
    val axi          = new AXI4LiteBundle(XLEN, XLEN) // 外部接口统一为 Lite Bundle
    val next_pc      = Input(UInt(XLEN.W))
    val pc_update_en = Input(Bool()) 
    val out          = Decoupled(new FetchPacket()) 
  })



  // [魔法拆分] 
  val AXI4Split(rBus, wBus) = io.axi

  // 连接读通道

  rBus.ar.bits := DontCare
  rBus.ar.valid := false.B
  rBus.r.ready := false.B

  // 屏蔽写通道 (Fetch 不写内存)
  wBus.aw.valid := false.B
  wBus.aw.bits  := DontCare
  wBus.w.valid  := false.B
  wBus.w.bits   := DontCare
  wBus.b.ready  := false.B


  io.out.valid := false.B
  io.out.bits := DontCare
  




  






  class Node extends Bundle {
    val addr = UInt(32.W)
    val inst = UInt(32.W)
    val valid = Bool()
  }

  object Node {
    def apply(addr: UInt, inst: UInt, valid: Bool) = {
      val res = Wire(new Node)
      res.addr := addr
      res.inst := inst
      res.valid := valid
      res
    }
  }

  class FetchTable {
    val size = 1

    val initNode = (new Node).Lit(
      _.addr -> 0.U,
      _.inst -> 0.U,
      _.valid -> false.B
    ) 

    val table = RegInit(VecInit(Seq.fill(size)(initNode)))


  
    def empty(): Bool = {
      !table.map(_.valid).reduce(_ || _)
    }
    def full(): Bool = {
      table.map(_.valid).reduce(_ && _)
    }

    def peek(): Node = {
      val valids = table.map(_.valid)
      // 对于 Size=1，First 和 Last 一样。
      // 对于 Size>1，作为 FIFO 应该取 PriorityEncoder(valids)
      val idx = PriorityEncoder(valids) 
      table(idx)
    }


    def push(data: Node): Bool = {

      val empties = table.map(n => !n.valid)
      val hasSpace = VecInit(empties).asUInt.orR

      val firstEmptyIndex = PriorityEncoder(empties)

      when(hasSpace) {
        table(firstEmptyIndex) := data
      }

      hasSpace
     
    }

    def pop(): Unit = {
      val valids = table.map(_.valid)
      val hasNode = VecInit(valids).asUInt.orR
      
      // 必须和 peek 的索引逻辑一致
      val idx = PriorityEncoder(valids) 
      
      when (hasNode) {
        table(idx).valid := false.B // 关键修正：必须写回 Invalid
      }
    }

    def hit(reqAddr: UInt): (UInt, Bool) = {
      val hits = table.map(n => n.valid && n.addr === reqAddr)

      val isHit = VecInit(hits).asUInt.orR

      val hitInst = Mux1H(hits, table.map(_.inst))

      (hitInst, isHit)
    }
  }
  
  val pc = RegInit(START_ADDR.U(XLEN.W))
  val fetchTable = new FetchTable
  val reqSent = RegInit(false.B)


  val fetchThread = new HardwareThread("Fetch_AXI_Core")
  val arValidProxy = fetchThread.driveManaged(rBus.ar.valid, false.B, false.B)
  val rReadyProxy  = fetchThread.driveManaged(rBus.r.ready,  false.B, false.B)
  fetchThread.startWhen(!fetchTable.full() && !reqSent)
  fetchThread.abortWhen(false.B)



  val fetchThreadPastRunning = RegNext(fetchThread.isRunning)
  when (fetchThread.isRunning && !fetchThreadPastRunning)  {
    printf("[DEBUG] [FETCH] fetchThread ONLINE!!!!\n")
    printf("fetching pc %x\n", pc)
  }

  when (fetchThreadPastRunning && !fetchThread.isRunning) {
    printf("[DEBUG] [FETCH] fetchThread OFFLINE!!!\n")
  }


  fetchThread.entry {
    val instData = Reg(UInt(32.W))
    fetchThread.Step {
      printf("[DEBUG] [FETCH] fetchThread step 1\n")

      fetchThread.write(arValidProxy, true.B) // 拉高 Valid
      
      rBus.ar.bits.addr  := pc
      rBus.ar.bits.id    := 0.U
      rBus.ar.bits.len   := 0.U
      rBus.ar.bits.size  := 2.U
      rBus.ar.bits.burst := 0.U
      // ... 其他 prot/cache 默认 0
      rBus.ar.bits.lock  := false.B
      rBus.ar.bits.cache := 0.U
      rBus.ar.bits.prot  := 0.U
      rBus.ar.bits.qos   := 0.U

      // 2. 等待握手
      fetchThread.pc := fetchThread.pc // 默认 hold
      when (rBus.ar.ready) {
        fetchThread.pc := fetchThread.pc + 1.U
      }
    }

    // --- Step 1: 接收数据 (R Channel) ---
    fetchThread.Step {
      printf("[DEBUG] [FETCH] fetchThread step 2\n")

      fetchThread.write(rReadyProxy, true.B)
      fetchThread.pc := fetchThread.pc // 默认 hold
      
      when (rBus.r.valid && rBus.r.bits.last && rBus.r.bits.id === 0.U) {
        instData := rBus.r.bits.data
        fetchThread.pc := fetchThread.pc + 1.U
      }
    }

    fetchThread.Step {
      printf("[DEBUG] [FETCH] fetchThread step 3\n")

      fetchTable.push(Node(pc, instData, true.B))
      printf("[DEBUG] [FETCH] fetchThread: DONE!!!!: push pc : %x, inst: %x\n", pc, instData)
      
      reqSent := true.B 
      

    }
  }



  when(io.pc_update_en) {
    pc := io.next_pc
    reqSent := false.B
  }

  val shootThread = new HardwareThread("Shoot_Core")
  
  // 托管 output valid
  val outValidProxy = shootThread.driveManaged(io.out.valid, false.B, false.B)
  
  // Start: 只要 Table 有东西，就尝试发射
  shootThread.startWhen(!fetchTable.empty())
  // Shoot 不需要 Abort，因为它只是单纯的搬运工，清空 Table 的逻辑由 FetchTable.pop 控制

  shootThread.entry {
    shootThread.Step {
      val node = fetchTable.peek()

      // 驱动接口
      shootThread.write(outValidProxy, true.B)
      io.out.bits.inst        := node.inst
      io.out.bits.pc          := node.addr
      io.out.bits.dnpc        := node.addr + 4.U
      io.out.bits.isException := false.B

      // 只有下游收了，才 Pop 并退出线程
      shootThread.pc := shootThread.pc
      when (io.out.ready) {
        fetchTable.pop()
        shootThread.pc := 0.U // 其实这里线程自动结束也可以，看 step 长度
      }
    }
  }

  


}