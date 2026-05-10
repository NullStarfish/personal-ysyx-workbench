package mycpu.core.bundles

import chisel3._


trait RAWDetect {
    def rd: UInt
    def rs1: ValidUIntView
    def rs2: ValidUIntView
}

class HazardPacket extends Bundle {
    val stall = Bool()
    val flush = Bool()
}