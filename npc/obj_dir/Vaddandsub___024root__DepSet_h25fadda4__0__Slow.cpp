// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub__Syms.h"
#include "Vaddandsub___024root.h"

VL_ATTR_COLD void Vaddandsub_Top___eval_initial__TOP__Top(Vaddandsub_Top* vlSelf);
VL_ATTR_COLD void Vaddandsub_rom___eval_initial__TOP__Top__imem_unit__rom0(Vaddandsub_rom* vlSelf);

VL_ATTR_COLD void Vaddandsub___024root___eval_initial(Vaddandsub___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddandsub___024root___eval_initial\n"); );
    // Body
    Vaddandsub_Top___eval_initial__TOP__Top((&vlSymsp->TOP__Top));
    Vaddandsub_rom___eval_initial__TOP__Top__imem_unit__rom0((&vlSymsp->TOP__Top__imem_unit__rom0));
    vlSelf->__Vtrigrprev__TOP__clk = vlSelf->clk;
    vlSelf->__Vtrigrprev__TOP__rst = vlSelf->rst;
}

#ifdef VL_DEBUG
VL_ATTR_COLD void Vaddandsub___024root___dump_triggers__stl(Vaddandsub___024root* vlSelf);
#endif  // VL_DEBUG

VL_ATTR_COLD void Vaddandsub___024root___eval_triggers__stl(Vaddandsub___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddandsub___024root___eval_triggers__stl\n"); );
    // Body
    vlSelf->__VstlTriggered.at(0U) = (0U == vlSelf->__VstlIterCount);
#ifdef VL_DEBUG
    if (VL_UNLIKELY(vlSymsp->_vm_contextp__->debug())) {
        Vaddandsub___024root___dump_triggers__stl(vlSelf);
    }
#endif
}

void Vaddandsub___024root___ico_sequent__TOP__0(Vaddandsub___024root* vlSelf);
VL_ATTR_COLD void Vaddandsub_Top___stl_sequent__TOP__Top__0(Vaddandsub_Top* vlSelf);
void Vaddandsub_rom___ico_sequent__TOP__Top__imem_unit__rom0__0(Vaddandsub_rom* vlSelf);
void Vaddandsub_Top___ico_sequent__TOP__Top__0(Vaddandsub_Top* vlSelf);

VL_ATTR_COLD void Vaddandsub___024root___eval_stl(Vaddandsub___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    Vaddandsub___024root___eval_stl\n"); );
    // Body
    if (vlSelf->__VstlTriggered.at(0U)) {
        Vaddandsub___024root___ico_sequent__TOP__0(vlSelf);
        Vaddandsub_Top___stl_sequent__TOP__Top__0((&vlSymsp->TOP__Top));
        Vaddandsub_rom___ico_sequent__TOP__Top__imem_unit__rom0__0((&vlSymsp->TOP__Top__imem_unit__rom0));
        Vaddandsub_Top___ico_sequent__TOP__Top__0((&vlSymsp->TOP__Top));
    }
}
