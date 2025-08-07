// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub__Syms.h"
#include "Vaddandsub_rom.h"

void Vaddandsub_rom___ctor_var_reset(Vaddandsub_rom* vlSelf);

Vaddandsub_rom::Vaddandsub_rom(Vaddandsub__Syms* symsp, const char* v__name)
    : VerilatedModule{v__name}
    , vlSymsp{symsp}
 {
    // Reset structure values
    Vaddandsub_rom___ctor_var_reset(this);
}

void Vaddandsub_rom::__Vconfigure(bool first) {
    if (false && first) {}  // Prevent unused
}

Vaddandsub_rom::~Vaddandsub_rom() {
}
