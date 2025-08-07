// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub__Syms.h"
#include "Vaddandsub_rom.h"

VL_INLINE_OPT void Vaddandsub_rom___ico_sequent__TOP__Top__imem_unit__rom0__0(Vaddandsub_rom* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+          Vaddandsub_rom___ico_sequent__TOP__Top__imem_unit__rom0__0\n"); );
    // Body
    vlSelf->__PVT__data = (((0x80000000U <= vlSymsp->TOP__Top.__PVT__pc_out) 
                            & (0x8000U > ((vlSymsp->TOP__Top.__PVT__pc_out 
                                           - (IData)(0x80000000U)) 
                                          >> 2U))) ? 
                           vlSelf->rom_data[(0x7fffU 
                                             & ((vlSymsp->TOP__Top.__PVT__pc_out 
                                                 - (IData)(0x80000000U)) 
                                                >> 2U))]
                            : 0U);
}
