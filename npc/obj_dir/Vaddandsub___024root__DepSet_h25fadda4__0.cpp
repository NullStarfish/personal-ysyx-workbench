// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub__Syms.h"
#include "Vaddandsub___024root.h"

#ifdef VL_DEBUG
VL_ATTR_COLD void Vaddandsub___024root___dump_triggers__ico(Vaddandsub___024root* vlSelf);
#endif  // VL_DEBUG

void Vaddandsub___024root___eval_triggers__ico(Vaddandsub___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddandsub___024root___eval_triggers__ico\n"); );
    // Body
    vlSelf->__VicoTriggered.at(0U) = (0U == vlSelf->__VicoIterCount);
#ifdef VL_DEBUG
    if (VL_UNLIKELY(vlSymsp->_vm_contextp__->debug())) {
        Vaddandsub___024root___dump_triggers__ico(vlSelf);
    }
#endif
}

void Vaddandsub___024root___ico_sequent__TOP__0(Vaddandsub___024root* vlSelf);
void Vaddandsub_rom___ico_sequent__TOP__Top__imem_unit__rom0__0(Vaddandsub_rom* vlSelf);
void Vaddandsub_Top___ico_sequent__TOP__Top__0(Vaddandsub_Top* vlSelf);

void Vaddandsub___024root___eval_ico(Vaddandsub___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddandsub___024root___eval_ico\n"); );
    // Body
    if (vlSelf->__VicoTriggered.at(0U)) {
        Vaddandsub___024root___ico_sequent__TOP__0(vlSelf);
        Vaddandsub_rom___ico_sequent__TOP__Top__imem_unit__rom0__0((&vlSymsp->TOP__Top__imem_unit__rom0));
        Vaddandsub_Top___ico_sequent__TOP__Top__0((&vlSymsp->TOP__Top));
    }
}

#ifdef VL_DEBUG
VL_ATTR_COLD void Vaddandsub___024root___dump_triggers__act(Vaddandsub___024root* vlSelf);
#endif  // VL_DEBUG

void Vaddandsub___024root___eval_triggers__act(Vaddandsub___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddandsub___024root___eval_triggers__act\n"); );
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
        Vaddandsub___024root___dump_triggers__act(vlSelf);
    }
#endif
}

void Vaddandsub_Top___nba_sequent__TOP__Top__0(Vaddandsub_Top* vlSelf);
void Vaddandsub_Top___nba_sequent__TOP__Top__1(Vaddandsub_Top* vlSelf);
void Vaddandsub_Top___nba_sequent__TOP__Top__2(Vaddandsub_Top* vlSelf);
void Vaddandsub_Top___nba_comb__TOP__Top__0(Vaddandsub_Top* vlSelf);

void Vaddandsub___024root___eval_nba(Vaddandsub___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddandsub___024root___eval_nba\n"); );
    // Body
    if (vlSelf->__VnbaTriggered.at(1U)) {
        Vaddandsub_Top___nba_sequent__TOP__Top__0((&vlSymsp->TOP__Top));
    }
    if (vlSelf->__VnbaTriggered.at(0U)) {
        Vaddandsub_Top___nba_sequent__TOP__Top__1((&vlSymsp->TOP__Top));
        Vaddandsub_rom___ico_sequent__TOP__Top__imem_unit__rom0__0((&vlSymsp->TOP__Top__imem_unit__rom0));
        Vaddandsub_Top___nba_sequent__TOP__Top__2((&vlSymsp->TOP__Top));
    }
    if ((vlSelf->__VnbaTriggered.at(0U) | vlSelf->__VnbaTriggered.at(1U))) {
        Vaddandsub_Top___nba_comb__TOP__Top__0((&vlSymsp->TOP__Top));
    }
}
