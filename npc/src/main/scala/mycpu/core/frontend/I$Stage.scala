package mycpu.core.frontend
import chisel3._
import chisel3.util._
import mycpu.cache._
import mycpu.core.bundles._
class I$Stage(
    params: CacheParams = CacheParams()
) extends Module {
   val io = new Bundle {
    val in = Flipped(Decoupled(new FetchPacket))
    val out = Decoupled(UInt(32.W))
    val redirectFlush = Input(Bool())
    val fencei = Input(Bool()) 
   } 
   val inflight = RegInit(false.B)
   io.in.ready := inflight === false.B
    when (io.in.fire) {
        inflight := true.B
    }
    

   val cacheSet = Module(new CacheSet(params)) 
    cacheSet.io.flush := io.fencei
    
    
   
}