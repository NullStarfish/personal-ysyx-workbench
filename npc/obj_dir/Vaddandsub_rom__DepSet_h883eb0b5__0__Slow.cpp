// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub_rom.h"

VL_ATTR_COLD void Vaddandsub_rom___eval_initial__TOP__Top__imem_unit__rom0(Vaddandsub_rom* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+          Vaddandsub_rom___eval_initial__TOP__Top__imem_unit__rom0\n"); );
    // Init
    IData/*31:0*/ __PVT__unnamedblk1__DOT__i;
    __PVT__unnamedblk1__DOT__i = 0;
    // Body
    __PVT__unnamedblk1__DOT__i = 0U;
    while (VL_GTS_III(32, 0x8000U, __PVT__unnamedblk1__DOT__i)) {
        vlSelf->rom_data[(0x7fffU & __PVT__unnamedblk1__DOT__i)] = 0x13U;
        __PVT__unnamedblk1__DOT__i = ((IData)(1U) + __PVT__unnamedblk1__DOT__i);
    }
}

VL_ATTR_COLD void Vaddandsub_rom___ctor_var_reset(Vaddandsub_rom* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+          Vaddandsub_rom___ctor_var_reset\n"); );
    // Body
    vlSelf->__PVT__addr = VL_RAND_RESET_I(32);
    vlSelf->__PVT__data = VL_RAND_RESET_I(32);
    for (int __Vi0 = 0; __Vi0 < 32768; ++__Vi0) {
        vlSelf->rom_data[__Vi0] = VL_RAND_RESET_I(32);
    }
}
