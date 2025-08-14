// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop__Syms.h"
#include "VTop___024root.h"

#ifdef VL_DEBUG
VL_ATTR_COLD void VTop___024root___dump_triggers__ico(VTop___024root* vlSelf);
#endif  // VL_DEBUG

void VTop___024root___eval_triggers__ico(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_triggers__ico\n"); );
    // Body
    vlSelf->__VicoTriggered.at(0U) = (0U == vlSelf->__VicoIterCount);
#ifdef VL_DEBUG
    if (VL_UNLIKELY(vlSymsp->_vm_contextp__->debug())) {
        VTop___024root___dump_triggers__ico(vlSelf);
    }
#endif
}

void VTop_Top___ico_sequent__TOP__Top__0(VTop_Top* vlSelf);
void VTop_RegFile___ico_sequent__TOP__Top__reg_file_unit__0(VTop_RegFile* vlSelf);
void VTop_Top___ico_sequent__TOP__Top__1(VTop_Top* vlSelf);

void VTop___024root___eval_ico(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_ico\n"); );
    // Body
    if (vlSelf->__VicoTriggered.at(0U)) {
        VTop_Top___ico_sequent__TOP__Top__0((&vlSymsp->TOP__Top));
        VTop_RegFile___ico_sequent__TOP__Top__reg_file_unit__0((&vlSymsp->TOP__Top__reg_file_unit));
        VTop_Top___ico_sequent__TOP__Top__1((&vlSymsp->TOP__Top));
    }
}

#ifdef VL_DEBUG
VL_ATTR_COLD void VTop___024root___dump_triggers__act(VTop___024root* vlSelf);
#endif  // VL_DEBUG

void VTop___024root___eval_triggers__act(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_triggers__act\n"); );
    // Body
    vlSelf->__VactTriggered.at(0U) = ((IData)(vlSelf->clk) 
                                      & (~ (IData)(vlSelf->__Vtrigrprev__TOP__clk)));
    vlSelf->__VactTriggered.at(1U) = (((~ (IData)(vlSelf->clk)) 
                                       & (IData)(vlSelf->__Vtrigrprev__TOP__clk)) 
                                      | ((IData)(vlSelf->rst) 
                                         & (~ (IData)(vlSelf->__Vtrigrprev__TOP__rst))));
    vlSelf->__Vtrigrprev__TOP__clk = vlSelf->clk;
    vlSelf->__Vtrigrprev__TOP__rst = vlSelf->rst;
#ifdef VL_DEBUG
    if (VL_UNLIKELY(vlSymsp->_vm_contextp__->debug())) {
        VTop___024root___dump_triggers__act(vlSelf);
    }
#endif
}

void VTop_Top___nba_sequent__TOP__Top__0(VTop_Top* vlSelf);
void VTop_Top___nba_sequent__TOP__Top__1(VTop_Top* vlSelf);
void VTop_RegFile___nba_sequent__TOP__Top__reg_file_unit__0(VTop_RegFile* vlSelf);
void VTop_Top___nba_sequent__TOP__Top__2(VTop_Top* vlSelf);
void VTop_Top___nba_sequent__TOP__Top__3(VTop_Top* vlSelf);
void VTop_Top___nba_sequent__TOP__Top__4(VTop_Top* vlSelf);

void VTop___024root___eval_nba(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_nba\n"); );
    // Body
    if (vlSelf->__VnbaTriggered.at(1U)) {
        VTop_Top___nba_sequent__TOP__Top__0((&vlSymsp->TOP__Top));
    }
    if (vlSelf->__VnbaTriggered.at(0U)) {
        VTop_Top___nba_sequent__TOP__Top__1((&vlSymsp->TOP__Top));
        VTop_RegFile___nba_sequent__TOP__Top__reg_file_unit__0((&vlSymsp->TOP__Top__reg_file_unit));
        VTop_Top___nba_sequent__TOP__Top__2((&vlSymsp->TOP__Top));
        VTop_RegFile___ico_sequent__TOP__Top__reg_file_unit__0((&vlSymsp->TOP__Top__reg_file_unit));
    }
    if (vlSelf->__VnbaTriggered.at(1U)) {
        VTop_Top___nba_sequent__TOP__Top__3((&vlSymsp->TOP__Top));
    }
    if (vlSelf->__VnbaTriggered.at(0U)) {
        VTop_Top___nba_sequent__TOP__Top__4((&vlSymsp->TOP__Top));
    }
}
