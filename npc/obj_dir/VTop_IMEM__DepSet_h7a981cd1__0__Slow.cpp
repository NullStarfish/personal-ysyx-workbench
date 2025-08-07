// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop_IMEM.h"

VL_ATTR_COLD void VTop_IMEM___ctor_var_reset(VTop_IMEM* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+        VTop_IMEM___ctor_var_reset\n"); );
    // Body
    vlSelf->__PVT__addr = VL_RAND_RESET_I(32);
    vlSelf->__PVT__inst = VL_RAND_RESET_I(32);
    vlSelf->__PVT__rd = VL_RAND_RESET_I(5);
    vlSelf->__PVT__rs1 = VL_RAND_RESET_I(5);
    vlSelf->__PVT__rs2 = VL_RAND_RESET_I(5);
}
