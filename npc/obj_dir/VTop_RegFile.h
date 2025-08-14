// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See VTop.h for the primary calling header

#ifndef VERILATED_VTOP_REGFILE_H_
#define VERILATED_VTOP_REGFILE_H_  // guard

#include "verilated.h"

class VTop__Syms;

class VTop_RegFile final : public VerilatedModule {
  public:

    // DESIGN SPECIFIC STATE
    VL_IN8(__PVT__clk,0,0);
    VL_IN8(__PVT__rst,0,0);
    VL_IN8(__PVT__AddrD,4,0);
    VL_IN8(__PVT__AddrA,4,0);
    VL_IN8(__PVT__AddrB,4,0);
    VL_IN8(__PVT__RegWEn,0,0);
    VL_IN8(__PVT__load_en,0,0);
    VL_IN(__PVT__DataD,31,0);
    VL_OUT(__PVT__DataA,31,0);
    VL_OUT(__PVT__DataB,31,0);
    VlUnpacked<IData/*31:0*/, 32> reg_file;

    // INTERNAL VARIABLES
    VTop__Syms* const vlSymsp;

    // CONSTRUCTORS
    VTop_RegFile(VTop__Syms* symsp, const char* v__name);
    ~VTop_RegFile();
    VL_UNCOPYABLE(VTop_RegFile);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
