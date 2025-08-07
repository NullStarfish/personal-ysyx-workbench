// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See Vaddandsub.h for the primary calling header

#ifndef VERILATED_VADDANDSUB___024ROOT_H_
#define VERILATED_VADDANDSUB___024ROOT_H_  // guard

#include "verilated.h"

class Vaddandsub__Syms;
class Vaddandsub_Top;
class Vaddandsub___024unit;


class Vaddandsub___024root final : public VerilatedModule {
  public:
    // CELLS
    Vaddandsub_Top* Top;
    Vaddandsub___024unit* __PVT____024unit;

    // DESIGN SPECIFIC STATE
    VL_IN8(clk,0,0);
    VL_IN8(rst,0,0);
    VL_IN8(BrUn,0,0);
    VL_OUT8(BrEq,0,0);
    VL_OUT8(BrLT,0,0);
    CData/*0:0*/ __Vtrigrprev__TOP__clk;
    CData/*0:0*/ __Vtrigrprev__TOP__rst;
    CData/*0:0*/ __VactContinue;
    VL_IN(rs1,31,0);
    VL_IN(rs2,31,0);
    IData/*31:0*/ __VstlIterCount;
    IData/*31:0*/ __VicoIterCount;
    IData/*31:0*/ __VactIterCount;
    VlTriggerVec<1> __VstlTriggered;
    VlTriggerVec<1> __VicoTriggered;
    VlTriggerVec<2> __VactTriggered;
    VlTriggerVec<2> __VnbaTriggered;

    // INTERNAL VARIABLES
    Vaddandsub__Syms* const vlSymsp;

    // CONSTRUCTORS
    Vaddandsub___024root(Vaddandsub__Syms* symsp, const char* v__name);
    ~Vaddandsub___024root();
    VL_UNCOPYABLE(Vaddandsub___024root);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
