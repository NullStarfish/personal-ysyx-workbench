package mycpu.core.bundles

import chisel3._




trait RAWRegInfo {
    def valid : Bool
    def addr  : UInt
}

class RAWRdPacket extends Bundle with RAWRegInfo{
    val valid = Bool()
    val addr = UInt(5.W)
}

class RAWRsPacket extends Bundle {
    val rs1 = new Bundle with RAWRegInfo {
        val valid = Bool()
        val addr = UInt(5.W)
    }
    val rs2 = new Bundle with RAWRegInfo {
        val valid = Bool()
        val addr = UInt(5.W)
    }
}

trait HazardResolv {
    def stall: Bool
    def flush: Bool
}
