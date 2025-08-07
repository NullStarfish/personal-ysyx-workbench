// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop__Syms.h"
#include "VTop_rom.h"

VL_INLINE_OPT void VTop_rom___ico_sequent__TOP__Top__imem_unit__rom0__0(VTop_rom* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+          VTop_rom___ico_sequent__TOP__Top__imem_unit__rom0__0\n"); );
    // Body
    vlSelf->__PVT__data = (((0x80000000U <= vlSymsp->TOP__Top.pc_out) 
                            & (0x8000U > ((vlSymsp->TOP__Top.pc_out 
                                           - (IData)(0x80000000U)) 
                                          >> 2U))) ? 
                           vlSelf->rom_data[(0x7fffU 
                                             & ((vlSymsp->TOP__Top.pc_out 
                                                 - (IData)(0x80000000U)) 
                                                >> 2U))]
                            : 0U);
}
