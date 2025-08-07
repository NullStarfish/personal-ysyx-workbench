// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop_Top.h"
#include "VTop__Syms.h"

void VTop_Top___ctor_var_reset(VTop_Top* vlSelf);

VTop_Top::VTop_Top(VTop__Syms* symsp, const char* v__name)
    : VerilatedModule{v__name}
    , vlSymsp{symsp}
 {
    // Reset structure values
    VTop_Top___ctor_var_reset(this);
}

void VTop_Top::__Vconfigure(bool first) {
    if (false && first) {}  // Prevent unused
}

VTop_Top::~VTop_Top() {
}
