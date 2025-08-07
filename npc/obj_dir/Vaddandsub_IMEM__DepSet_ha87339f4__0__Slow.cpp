// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub_IMEM.h"

VL_ATTR_COLD void Vaddandsub_IMEM___ctor_var_reset(Vaddandsub_IMEM* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+        Vaddandsub_IMEM___ctor_var_reset\n"); );
    // Body
    vlSelf->__PVT__addr = VL_RAND_RESET_I(32);
    vlSelf->__PVT__inst = VL_RAND_RESET_I(32);
    vlSelf->__PVT__rd = VL_RAND_RESET_I(5);
    vlSelf->__PVT__rs1 = VL_RAND_RESET_I(5);
    vlSelf->__PVT__rs2 = VL_RAND_RESET_I(5);
    vlSelf->__PVT____pinNumber6 = VL_RAND_RESET_I(1);
}
