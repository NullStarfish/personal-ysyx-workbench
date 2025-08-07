// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub__Syms.h"
#include "Vaddandsub___024unit.h"

void Vaddandsub___024unit___ctor_var_reset(Vaddandsub___024unit* vlSelf);

Vaddandsub___024unit::Vaddandsub___024unit(Vaddandsub__Syms* symsp, const char* v__name)
    : VerilatedModule{v__name}
    , vlSymsp{symsp}
 {
    // Reset structure values
    Vaddandsub___024unit___ctor_var_reset(this);
}

void Vaddandsub___024unit::__Vconfigure(bool first) {
    if (false && first) {}  // Prevent unused
}

Vaddandsub___024unit::~Vaddandsub___024unit() {
}
