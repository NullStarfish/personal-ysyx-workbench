// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See Vaddandsub.h for the primary calling header

#ifndef VERILATED_VADDANDSUB_ROM_H_
#define VERILATED_VADDANDSUB_ROM_H_  // guard

#include "verilated.h"

class Vaddandsub__Syms;

class Vaddandsub_rom final : public VerilatedModule {
  public:

    // DESIGN SPECIFIC STATE
    VL_IN(__PVT__addr,31,0);
    VL_OUT(__PVT__data,31,0);
    VlUnpacked<IData/*31:0*/, 32768> rom_data;

    // INTERNAL VARIABLES
    Vaddandsub__Syms* const vlSymsp;

    // CONSTRUCTORS
    Vaddandsub_rom(Vaddandsub__Syms* symsp, const char* v__name);
    ~Vaddandsub_rom();
    VL_UNCOPYABLE(Vaddandsub_rom);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
