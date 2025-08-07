// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub_Top.h"

VL_INLINE_OPT void Vaddandsub_Top___nba_comb__TOP__Top__0(Vaddandsub_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      Vaddandsub_Top___nba_comb__TOP__Top__0\n"); );
    // Body
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__WBSel) 
                       == vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__WBSel) == vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__WBSel) 
                          == vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__WBSel) == vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__WBSel) 
                          == vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__WBSel) == vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__wb_data = ((IData)(vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit)
                               ? vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out
                               : 0U);
}
