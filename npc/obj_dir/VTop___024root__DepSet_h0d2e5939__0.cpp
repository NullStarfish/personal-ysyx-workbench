// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop___024root.h"

void VTop___024root___eval_act(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_act\n"); );
}

VL_INLINE_OPT void VTop___024root___nba_sequent__TOP__0(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___nba_sequent__TOP__0\n"); );
    // Init
    SData/*9:0*/ __Vdlyvdim0__Top__DOT__dmem_unit__DOT__mem__v0;
    __Vdlyvdim0__Top__DOT__dmem_unit__DOT__mem__v0 = 0;
    IData/*31:0*/ __Vdlyvval__Top__DOT__dmem_unit__DOT__mem__v0;
    __Vdlyvval__Top__DOT__dmem_unit__DOT__mem__v0 = 0;
    CData/*0:0*/ __Vdlyvset__Top__DOT__dmem_unit__DOT__mem__v0;
    __Vdlyvset__Top__DOT__dmem_unit__DOT__mem__v0 = 0;
    // Body
    __Vdlyvset__Top__DOT__dmem_unit__DOT__mem__v0 = 0U;
    if ((1U & (~ (IData)(vlSelf->rst)))) {
        if ((0x23U == (0x7fU & vlSelf->Top__DOT__inst))) {
            __Vdlyvval__Top__DOT__dmem_unit__DOT__mem__v0 
                = vlSelf->Top__DOT__reg_rs2_data;
            __Vdlyvset__Top__DOT__dmem_unit__DOT__mem__v0 = 1U;
            __Vdlyvdim0__Top__DOT__dmem_unit__DOT__mem__v0 
                = (0x3ffU & (vlSelf->Top__DOT__alu_result 
                             >> 2U));
        }
    }
    if (vlSelf->rst) {
        vlSelf->Top__DOT__dmem_rdata = 0U;
    } else if ((0x23U != (0x7fU & vlSelf->Top__DOT__inst))) {
        vlSelf->Top__DOT__dmem_rdata = vlSelf->Top__DOT__dmem_unit__DOT__mem
            [(0x3ffU & (vlSelf->Top__DOT__alu_result 
                        >> 2U))];
    }
    if (__Vdlyvset__Top__DOT__dmem_unit__DOT__mem__v0) {
        vlSelf->Top__DOT__dmem_unit__DOT__mem[__Vdlyvdim0__Top__DOT__dmem_unit__DOT__mem__v0] 
            = __Vdlyvval__Top__DOT__dmem_unit__DOT__mem__v0;
    }
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[2U] 
        = vlSelf->Top__DOT__dmem_rdata;
}

VL_INLINE_OPT void VTop___024root___nba_comb__TOP__0(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___nba_comb__TOP__0\n"); );
    // Body
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->Top__DOT__WBSel) 
                       == vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__WBSel) == vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->Top__DOT__WBSel) 
                          == vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->Top__DOT__WBSel) == vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->Top__DOT__WBSel) 
                          == vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->Top__DOT__WBSel) == vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->Top__DOT__wb_data = ((IData)(vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit)
                                  ? vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out
                                  : 0U);
}

void VTop___024root___nba_sequent__TOP__1(VTop___024root* vlSelf);

void VTop___024root___eval_nba(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_nba\n"); );
    // Body
    if (vlSelf->__VnbaTriggered.at(1U)) {
        VTop___024root___nba_sequent__TOP__0(vlSelf);
    }
    if (vlSelf->__VnbaTriggered.at(0U)) {
        VTop___024root___nba_sequent__TOP__1(vlSelf);
    }
    if ((vlSelf->__VnbaTriggered.at(0U) | vlSelf->__VnbaTriggered.at(1U))) {
        VTop___024root___nba_comb__TOP__0(vlSelf);
    }
}

void VTop___024root___eval_triggers__act(VTop___024root* vlSelf);
#ifdef VL_DEBUG
VL_ATTR_COLD void VTop___024root___dump_triggers__act(VTop___024root* vlSelf);
#endif  // VL_DEBUG
#ifdef VL_DEBUG
VL_ATTR_COLD void VTop___024root___dump_triggers__nba(VTop___024root* vlSelf);
#endif  // VL_DEBUG

void VTop___024root___eval(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval\n"); );
    // Init
    VlTriggerVec<2> __VpreTriggered;
    IData/*31:0*/ __VnbaIterCount;
    CData/*0:0*/ __VnbaContinue;
    // Body
    __VnbaIterCount = 0U;
    __VnbaContinue = 1U;
    while (__VnbaContinue) {
        __VnbaContinue = 0U;
        vlSelf->__VnbaTriggered.clear();
        vlSelf->__VactIterCount = 0U;
        vlSelf->__VactContinue = 1U;
        while (vlSelf->__VactContinue) {
            vlSelf->__VactContinue = 0U;
            VTop___024root___eval_triggers__act(vlSelf);
            if (vlSelf->__VactTriggered.any()) {
                vlSelf->__VactContinue = 1U;
                if (VL_UNLIKELY((0x64U < vlSelf->__VactIterCount))) {
#ifdef VL_DEBUG
                    VTop___024root___dump_triggers__act(vlSelf);
#endif
                    VL_FATAL_MT("vsrc/Top.v", 4, "", "Active region did not converge.");
                }
                vlSelf->__VactIterCount = ((IData)(1U) 
                                           + vlSelf->__VactIterCount);
                __VpreTriggered.andNot(vlSelf->__VactTriggered, vlSelf->__VnbaTriggered);
                vlSelf->__VnbaTriggered.set(vlSelf->__VactTriggered);
                VTop___024root___eval_act(vlSelf);
            }
        }
        if (vlSelf->__VnbaTriggered.any()) {
            __VnbaContinue = 1U;
            if (VL_UNLIKELY((0x64U < __VnbaIterCount))) {
#ifdef VL_DEBUG
                VTop___024root___dump_triggers__nba(vlSelf);
#endif
                VL_FATAL_MT("vsrc/Top.v", 4, "", "NBA region did not converge.");
            }
            __VnbaIterCount = ((IData)(1U) + __VnbaIterCount);
            VTop___024root___eval_nba(vlSelf);
        }
    }
}

#ifdef VL_DEBUG
void VTop___024root___eval_debug_assertions(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_debug_assertions\n"); );
    // Body
    if (VL_UNLIKELY((vlSelf->clk & 0xfeU))) {
        Verilated::overWidthError("clk");}
    if (VL_UNLIKELY((vlSelf->rst & 0xfeU))) {
        Verilated::overWidthError("rst");}
}
#endif  // VL_DEBUG
