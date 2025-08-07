// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub_IMEM.h"
#include "Vaddandsub__Syms.h"

void Vaddandsub_IMEM___ctor_var_reset(Vaddandsub_IMEM* vlSelf);

Vaddandsub_IMEM::Vaddandsub_IMEM(Vaddandsub__Syms* symsp, const char* v__name)
    : VerilatedModule{v__name}
    , vlSymsp{symsp}
 {
    // Reset structure values
    Vaddandsub_IMEM___ctor_var_reset(this);
}

void Vaddandsub_IMEM::__Vconfigure(bool first) {
    if (false && first) {}  // Prevent unused
}

Vaddandsub_IMEM::~Vaddandsub_IMEM() {
}
