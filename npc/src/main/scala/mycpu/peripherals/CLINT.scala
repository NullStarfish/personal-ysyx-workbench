package mycpu.peripherals
import chisel3._
import chisel3.util._
import mycpu._
import mycpu.peripherals._
import mycpu.utils._

class CLINT extends Peripheral(MemMap.devices(2)) {
    io.bus.setAsSlave()

    
}