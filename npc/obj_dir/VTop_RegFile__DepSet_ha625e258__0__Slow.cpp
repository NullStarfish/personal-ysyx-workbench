// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop_RegFile.h"

VL_ATTR_COLD void VTop_RegFile___ctor_var_reset(VTop_RegFile* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+        VTop_RegFile___ctor_var_reset\n"); );
    // Body
    vlSelf->__PVT__clk = VL_RAND_RESET_I(1);
    vlSelf->__PVT__rst = VL_RAND_RESET_I(1);
    vlSelf->__PVT__DataD = VL_RAND_RESET_I(32);
    vlSelf->__PVT__AddrD = VL_RAND_RESET_I(5);
    vlSelf->__PVT__AddrA = VL_RAND_RESET_I(5);
    vlSelf->__PVT__AddrB = VL_RAND_RESET_I(5);
    vlSelf->__PVT__DataA = VL_RAND_RESET_I(32);
    vlSelf->__PVT__DataB = VL_RAND_RESET_I(32);
    vlSelf->__PVT__RegWEn = VL_RAND_RESET_I(1);
    vlSelf->__PVT__load_en = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 32; ++__Vi0) {
        vlSelf->reg_file[__Vi0] = VL_RAND_RESET_I(32);
    }
}
