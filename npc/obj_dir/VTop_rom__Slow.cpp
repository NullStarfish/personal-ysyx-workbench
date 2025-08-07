// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop__Syms.h"
#include "VTop_rom.h"

void VTop_rom___ctor_var_reset(VTop_rom* vlSelf);

VTop_rom::VTop_rom(VTop__Syms* symsp, const char* v__name)
    : VerilatedModule{v__name}
    , vlSymsp{symsp}
 {
    // Reset structure values
    VTop_rom___ctor_var_reset(this);
}

void VTop_rom::__Vconfigure(bool first) {
    if (false && first) {}  // Prevent unused
}

VTop_rom::~VTop_rom() {
}
