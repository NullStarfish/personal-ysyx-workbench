// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See Vaddandsub.h for the primary calling header

#ifndef VERILATED_VADDANDSUB___024UNIT_H_
#define VERILATED_VADDANDSUB___024UNIT_H_  // guard

#include "verilated.h"

class Vaddandsub__Syms;

class Vaddandsub___024unit final : public VerilatedModule {
  public:

    // INTERNAL VARIABLES
    Vaddandsub__Syms* const vlSymsp;

    // CONSTRUCTORS
    Vaddandsub___024unit(Vaddandsub__Syms* symsp, const char* v__name);
    ~Vaddandsub___024unit();
    VL_UNCOPYABLE(Vaddandsub___024unit);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
