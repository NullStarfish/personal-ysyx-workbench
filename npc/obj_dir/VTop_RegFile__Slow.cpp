// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop_RegFile.h"
#include "VTop__Syms.h"

void VTop_RegFile___ctor_var_reset(VTop_RegFile* vlSelf);

VTop_RegFile::VTop_RegFile(VTop__Syms* symsp, const char* v__name)
    : VerilatedModule{v__name}
    , vlSymsp{symsp}
 {
    // Reset structure values
    VTop_RegFile___ctor_var_reset(this);
}

void VTop_RegFile::__Vconfigure(bool first) {
    if (false && first) {}  // Prevent unused
}

VTop_RegFile::~VTop_RegFile() {
}
