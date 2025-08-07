// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub__Syms.h"
#include "Vaddandsub___024unit.h"

extern "C" int ebreak();

VL_INLINE_OPT void Vaddandsub___024unit____Vdpiimwrap_ebreak_TOP____024unit(IData/*31:0*/ &ebreak__Vfuncrtn) {
    VL_DEBUG_IF(VL_DBG_MSGF("+        Vaddandsub___024unit____Vdpiimwrap_ebreak_TOP____024unit\n"); );
    // Body
    int ebreak__Vfuncrtn__Vcvt;
    ebreak__Vfuncrtn__Vcvt = ebreak();
    ebreak__Vfuncrtn = ebreak__Vfuncrtn__Vcvt;
}
