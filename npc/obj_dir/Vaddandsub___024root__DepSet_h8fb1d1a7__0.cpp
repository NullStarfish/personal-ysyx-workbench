// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub___024root.h"

VL_INLINE_OPT void Vaddandsub___024root___ico_sequent__TOP__0(Vaddandsub___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddandsub___024root___ico_sequent__TOP__0\n"); );
    // Body
    vlSelf->BrEq = (vlSelf->rs1 == vlSelf->rs2);
    vlSelf->BrLT = ((IData)(vlSelf->BrUn) ? (vlSelf->rs1 
                                             < vlSelf->rs2)
                     : VL_LTS_III(32, vlSelf->rs1, vlSelf->rs2));
}

void Vaddandsub___024root___eval_act(Vaddandsub___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddandsub___024root___eval_act\n"); );
}

void Vaddandsub___024root___eval_triggers__ico(Vaddandsub___024root* vlSelf);
#ifdef VL_DEBUG
VL_ATTR_COLD void Vaddandsub___024root___dump_triggers__ico(Vaddandsub___024root* vlSelf);
#endif  // VL_DEBUG
void Vaddandsub___024root___eval_ico(Vaddandsub___024root* vlSelf);
void Vaddandsub___024root___eval_triggers__act(Vaddandsub___024root* vlSelf);
#ifdef VL_DEBUG
VL_ATTR_COLD void Vaddandsub___024root___dump_triggers__act(Vaddandsub___024root* vlSelf);
#endif  // VL_DEBUG
#ifdef VL_DEBUG
VL_ATTR_COLD void Vaddandsub___024root___dump_triggers__nba(Vaddandsub___024root* vlSelf);
#endif  // VL_DEBUG
void Vaddandsub___024root___eval_nba(Vaddandsub___024root* vlSelf);

void Vaddandsub___024root___eval(Vaddandsub___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddandsub___024root___eval\n"); );
    // Init
    CData/*0:0*/ __VicoContinue;
    VlTriggerVec<2> __VpreTriggered;
    IData/*31:0*/ __VnbaIterCount;
    CData/*0:0*/ __VnbaContinue;
    // Body
    vlSelf->__VicoIterCount = 0U;
    __VicoContinue = 1U;
    while (__VicoContinue) {
        __VicoContinue = 0U;
        Vaddandsub___024root___eval_triggers__ico(vlSelf);
        if (vlSelf->__VicoTriggered.any()) {
            __VicoContinue = 1U;
            if (VL_UNLIKELY((0x64U < vlSelf->__VicoIterCount))) {
#ifdef VL_DEBUG
                Vaddandsub___024root___dump_triggers__ico(vlSelf);
#endif
                VL_FATAL_MT("vsrc/BranchComp.v", 1, "", "Input combinational region did not converge.");
            }
            vlSelf->__VicoIterCount = ((IData)(1U) 
                                       + vlSelf->__VicoIterCount);
            Vaddandsub___024root___eval_ico(vlSelf);
        }
    }
    __VnbaIterCount = 0U;
    __VnbaContinue = 1U;
    while (__VnbaContinue) {
        __VnbaContinue = 0U;
        vlSelf->__VnbaTriggered.clear();
        vlSelf->__VactIterCount = 0U;
        vlSelf->__VactContinue = 1U;
        while (vlSelf->__VactContinue) {
            vlSelf->__VactContinue = 0U;
            Vaddandsub___024root___eval_triggers__act(vlSelf);
            if (vlSelf->__VactTriggered.any()) {
                vlSelf->__VactContinue = 1U;
                if (VL_UNLIKELY((0x64U < vlSelf->__VactIterCount))) {
#ifdef VL_DEBUG
                    Vaddandsub___024root___dump_triggers__act(vlSelf);
#endif
                    VL_FATAL_MT("vsrc/BranchComp.v", 1, "", "Active region did not converge.");
                }
                vlSelf->__VactIterCount = ((IData)(1U) 
                                           + vlSelf->__VactIterCount);
                __VpreTriggered.andNot(vlSelf->__VactTriggered, vlSelf->__VnbaTriggered);
                vlSelf->__VnbaTriggered.set(vlSelf->__VactTriggered);
                Vaddandsub___024root___eval_act(vlSelf);
            }
        }
        if (vlSelf->__VnbaTriggered.any()) {
            __VnbaContinue = 1U;
            if (VL_UNLIKELY((0x64U < __VnbaIterCount))) {
#ifdef VL_DEBUG
                Vaddandsub___024root___dump_triggers__nba(vlSelf);
#endif
                VL_FATAL_MT("vsrc/BranchComp.v", 1, "", "NBA region did not converge.");
            }
            __VnbaIterCount = ((IData)(1U) + __VnbaIterCount);
            Vaddandsub___024root___eval_nba(vlSelf);
        }
    }
}

#ifdef VL_DEBUG
void Vaddandsub___024root___eval_debug_assertions(Vaddandsub___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddandsub___024root___eval_debug_assertions\n"); );
    // Body
    if (VL_UNLIKELY((vlSelf->BrUn & 0xfeU))) {
        Verilated::overWidthError("BrUn");}
    if (VL_UNLIKELY((vlSelf->clk & 0xfeU))) {
        Verilated::overWidthError("clk");}
    if (VL_UNLIKELY((vlSelf->rst & 0xfeU))) {
        Verilated::overWidthError("rst");}
}
#endif  // VL_DEBUG
