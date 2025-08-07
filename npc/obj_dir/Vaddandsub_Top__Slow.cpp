// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub_Top.h"
#include "Vaddandsub__Syms.h"

void Vaddandsub_Top___ctor_var_reset(Vaddandsub_Top* vlSelf);

Vaddandsub_Top::Vaddandsub_Top(Vaddandsub__Syms* symsp, const char* v__name)
    : VerilatedModule{v__name}
    , vlSymsp{symsp}
 {
    // Reset structure values
    Vaddandsub_Top___ctor_var_reset(this);
}

void Vaddandsub_Top::__Vconfigure(bool first) {
    if (false && first) {}  // Prevent unused
}

Vaddandsub_Top::~Vaddandsub_Top() {
}
