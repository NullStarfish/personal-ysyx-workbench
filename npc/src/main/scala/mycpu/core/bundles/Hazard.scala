package mycpu.core.bundles

import chisel3._


trait RAWDetect {
    def rd: UInt
    def rs1: ValidUIntView
    def rs2: ValidUIntView
}

class HazardRdInfo extends Bundle {
    val valid = Bool()
    val rd = UInt(5.W)
}

class HazardRsInfo extends Bundle {
    val rs1 = new Bundle with ValidUIntView {
        val valid = Bool()
        val bits = UInt(5.W)
    }
    val rs2 = new Bundle with ValidUIntView {
        val valid = Bool()
        val bits = UInt(5.W)
    }
}

class HazardPacket extends Bundle {
    val stall = Bool()
    val flush = Bool()
}
