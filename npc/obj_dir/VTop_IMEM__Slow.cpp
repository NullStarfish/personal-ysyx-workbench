// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop_IMEM.h"
#include "VTop__Syms.h"

void VTop_IMEM___ctor_var_reset(VTop_IMEM* vlSelf);

VTop_IMEM::VTop_IMEM(VTop__Syms* symsp, const char* v__name)
    : VerilatedModule{v__name}
    , vlSymsp{symsp}
 {
    // Reset structure values
    VTop_IMEM___ctor_var_reset(this);
}

void VTop_IMEM::__Vconfigure(bool first) {
    if (false && first) {}  // Prevent unused
}

VTop_IMEM::~VTop_IMEM() {
}
