// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop___024root.h"

VL_ATTR_COLD void VTop___024root___eval_static(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_static\n"); );
}

VL_ATTR_COLD void VTop___024root___eval_initial__TOP(VTop___024root* vlSelf);

VL_ATTR_COLD void VTop___024root___eval_initial(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_initial\n"); );
    // Body
    VTop___024root___eval_initial__TOP(vlSelf);
    vlSelf->__Vtrigrprev__TOP__clk = vlSelf->clk;
    vlSelf->__Vtrigrprev__TOP__rst = vlSelf->rst;
}

VL_ATTR_COLD void VTop___024root___eval_initial__TOP(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_initial__TOP\n"); );
    // Init
    IData/*31:0*/ Top__DOT__imem_unit__DOT__rom0__DOT__unnamedblk1__DOT__i;
    Top__DOT__imem_unit__DOT__rom0__DOT__unnamedblk1__DOT__i = 0;
    // Body
    vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list[0U] = 0x63U;
    vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list[1U] = 0x33U;
    vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list[0U] = 6U;
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[0U] = 7U;
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[1U] = 6U;
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[2U] = 5U;
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[3U] = 4U;
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[4U] = 1U;
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[5U] = 0U;
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[0U] = 5U;
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[1U] = 4U;
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[2U] = 3U;
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[3U] = 2U;
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[4U] = 1U;
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[5U] = 0U;
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list[0U] = 1U;
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list[1U] = 0U;
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list[0U] = 1U;
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list[1U] = 0U;
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list[0U] = 2U;
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list[1U] = 1U;
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list[2U] = 0U;
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list[0U] = 0x67U;
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list[1U] = 0x6fU;
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list[2U] = 3U;
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list[0U] = 2U;
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list[1U] = 2U;
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list[2U] = 0U;
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list[0U] = 0x6fU;
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list[1U] = 0x63U;
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list[2U] = 0x23U;
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list[3U] = 0x17U;
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list[4U] = 0x37U;
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list[0U] = 4U;
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list[1U] = 3U;
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list[2U] = 2U;
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list[3U] = 0U;
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list[4U] = 0U;
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__key_list[0U] = 6U;
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__key_list[1U] = 1U;
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__key_list[2U] = 0U;
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__data_list[0U] = 1U;
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__data_list[1U] = 1U;
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__data_list[2U] = 0U;
    Top__DOT__imem_unit__DOT__rom0__DOT__unnamedblk1__DOT__i = 0U;
    while (VL_GTS_III(32, 0x100U, Top__DOT__imem_unit__DOT__rom0__DOT__unnamedblk1__DOT__i)) {
        vlSelf->Top__DOT__imem_unit__DOT__rom0__DOT__rom_data[(0xffU 
                                                               & Top__DOT__imem_unit__DOT__rom0__DOT__unnamedblk1__DOT__i)] = 0x13U;
        Top__DOT__imem_unit__DOT__rom0__DOT__unnamedblk1__DOT__i 
            = ((IData)(1U) + Top__DOT__imem_unit__DOT__rom0__DOT__unnamedblk1__DOT__i);
    }
    vlSelf->Top__DOT__imem_unit__DOT__rom0__DOT__rom_data[0U] = 0xaaaaa2b7U;
    vlSelf->Top__DOT__imem_unit__DOT__rom0__DOT__rom_data[1U] = 0x55528293U;
    vlSelf->Top__DOT__imem_unit__DOT__rom0__DOT__rom_data[2U] = 0x6400313U;
    vlSelf->Top__DOT__imem_unit__DOT__rom0__DOT__rom_data[3U] = 0x532623U;
    vlSelf->Top__DOT__imem_unit__DOT__rom0__DOT__rom_data[4U] = 0xc32383U;
    vlSelf->Top__DOT__imem_unit__DOT__rom0__DOT__rom_data[5U] = 0x728863U;
    vlSelf->Top__DOT__imem_unit__DOT__rom0__DOT__rom_data[6U] = 0x100113U;
    vlSelf->Top__DOT__imem_unit__DOT__rom0__DOT__rom_data[7U] = 0x200113U;
    vlSelf->Top__DOT__imem_unit__DOT__rom0__DOT__rom_data[8U] = 0x100073U;
}

VL_ATTR_COLD void VTop___024root___eval_final(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_final\n"); );
}

VL_ATTR_COLD void VTop___024root___eval_triggers__stl(VTop___024root* vlSelf);
#ifdef VL_DEBUG
VL_ATTR_COLD void VTop___024root___dump_triggers__stl(VTop___024root* vlSelf);
#endif  // VL_DEBUG
VL_ATTR_COLD void VTop___024root___eval_stl(VTop___024root* vlSelf);

VL_ATTR_COLD void VTop___024root___eval_settle(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_settle\n"); );
    // Init
    CData/*0:0*/ __VstlContinue;
    // Body
    vlSelf->__VstlIterCount = 0U;
    __VstlContinue = 1U;
    while (__VstlContinue) {
        __VstlContinue = 0U;
        VTop___024root___eval_triggers__stl(vlSelf);
        if (vlSelf->__VstlTriggered.any()) {
            __VstlContinue = 1U;
            if (VL_UNLIKELY((0x64U < vlSelf->__VstlIterCount))) {
#ifdef VL_DEBUG
                VTop___024root___dump_triggers__stl(vlSelf);
#endif
                VL_FATAL_MT("vsrc/Top.v", 4, "", "Settle region did not converge.");
            }
            vlSelf->__VstlIterCount = ((IData)(1U) 
                                       + vlSelf->__VstlIterCount);
            VTop___024root___eval_stl(vlSelf);
        }
    }
}

#ifdef VL_DEBUG
VL_ATTR_COLD void VTop___024root___dump_triggers__stl(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___dump_triggers__stl\n"); );
    // Body
    if ((1U & (~ (IData)(vlSelf->__VstlTriggered.any())))) {
        VL_DBG_MSGF("         No triggers active\n");
    }
    if (vlSelf->__VstlTriggered.at(0U)) {
        VL_DBG_MSGF("         'stl' region trigger index 0 is active: Internal 'stl' trigger - first iteration\n");
    }
}
#endif  // VL_DEBUG

VL_ATTR_COLD void VTop___024root___stl_sequent__TOP__0(VTop___024root* vlSelf);

VL_ATTR_COLD void VTop___024root___eval_stl(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_stl\n"); );
    // Body
    if (vlSelf->__VstlTriggered.at(0U)) {
        VTop___024root___stl_sequent__TOP__0(vlSelf);
    }
}

#ifdef VL_DEBUG
VL_ATTR_COLD void VTop___024root___dump_triggers__act(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___dump_triggers__act\n"); );
    // Body
    if ((1U & (~ (IData)(vlSelf->__VactTriggered.any())))) {
        VL_DBG_MSGF("         No triggers active\n");
    }
    if (vlSelf->__VactTriggered.at(0U)) {
        VL_DBG_MSGF("         'act' region trigger index 0 is active: @(posedge clk)\n");
    }
    if (vlSelf->__VactTriggered.at(1U)) {
        VL_DBG_MSGF("         'act' region trigger index 1 is active: @(negedge clk or posedge rst)\n");
    }
}
#endif  // VL_DEBUG

#ifdef VL_DEBUG
VL_ATTR_COLD void VTop___024root___dump_triggers__nba(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___dump_triggers__nba\n"); );
    // Body
    if ((1U & (~ (IData)(vlSelf->__VnbaTriggered.any())))) {
        VL_DBG_MSGF("         No triggers active\n");
    }
    if (vlSelf->__VnbaTriggered.at(0U)) {
        VL_DBG_MSGF("         'nba' region trigger index 0 is active: @(posedge clk)\n");
    }
    if (vlSelf->__VnbaTriggered.at(1U)) {
        VL_DBG_MSGF("         'nba' region trigger index 1 is active: @(negedge clk or posedge rst)\n");
    }
}
#endif  // VL_DEBUG

VL_ATTR_COLD void VTop___024root___ctor_var_reset(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___ctor_var_reset\n"); );
    // Body
    vlSelf->clk = VL_RAND_RESET_I(1);
    vlSelf->rst = VL_RAND_RESET_I(1);
    vlSelf->Top__DOT__pc_out = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__inst = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__opcode = VL_RAND_RESET_I(7);
    vlSelf->Top__DOT__Asel = VL_RAND_RESET_I(1);
    vlSelf->Top__DOT__Bsel = VL_RAND_RESET_I(1);
    vlSelf->Top__DOT__BrUn = VL_RAND_RESET_I(1);
    vlSelf->Top__DOT__WBSel = VL_RAND_RESET_I(2);
    vlSelf->Top__DOT__ImmSel = VL_RAND_RESET_I(3);
    vlSelf->Top__DOT__ALUSel = VL_RAND_RESET_I(3);
    vlSelf->Top__DOT__rs1_addr = VL_RAND_RESET_I(5);
    vlSelf->Top__DOT__reg_rs2_data = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__wb_data = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__imm_out = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__alu_in_a = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__alu_in_b = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__alu_result = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__alu_br_lt = VL_RAND_RESET_I(1);
    vlSelf->Top__DOT__dmem_rdata = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__pc_unit__DOT__PCin = VL_RAND_RESET_I(32);
    for (int __Vi0 = 0; __Vi0 < 256; ++__Vi0) {
        vlSelf->Top__DOT__imem_unit__DOT__rom0__DOT__rom_data[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->Top__DOT__controller_unit__DOT__take_branch = VL_RAND_RESET_I(1);
    vlSelf->Top__DOT__controller_unit__DOT____VdfgTmp_h7acf63f3__0 = 0;
    for (int __Vi0 = 0; __Vi0 < 3; ++__Vi0) {
        vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(7);
    }
    for (int __Vi0 = 0; __Vi0 < 3; ++__Vi0) {
        vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(2);
    }
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(2);
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 5; ++__Vi0) {
        vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(7);
    }
    for (int __Vi0 = 0; __Vi0 < 5; ++__Vi0) {
        vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(3);
    }
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(3);
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 2; ++__Vi0) {
        vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(7);
    }
    for (int __Vi0 = 0; __Vi0 < 2; ++__Vi0) {
        vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(3);
    }
    vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(3);
    vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 6; ++__Vi0) {
        vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(3);
    }
    for (int __Vi0 = 0; __Vi0 < 6; ++__Vi0) {
        vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(1);
    }
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(1);
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 32; ++__Vi0) {
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[__Vi0] = VL_RAND_RESET_I(32);
    }
    for (int __Vi0 = 0; __Vi0 < 6; ++__Vi0) {
        vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(3);
    }
    for (int __Vi0 = 0; __Vi0 < 6; ++__Vi0) {
        vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 2; ++__Vi0) {
        vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(1);
    }
    for (int __Vi0 = 0; __Vi0 < 2; ++__Vi0) {
        vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(32);
    for (int __Vi0 = 0; __Vi0 < 2; ++__Vi0) {
        vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(1);
    }
    for (int __Vi0 = 0; __Vi0 < 2; ++__Vi0) {
        vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__alu_unit__DOT__AdderCtrl = VL_RAND_RESET_I(1);
    vlSelf->Top__DOT__alu_unit__DOT__AdderResult = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__alu_unit__DOT__internal_overflow = VL_RAND_RESET_I(1);
    vlSelf->Top__DOT__alu_unit__DOT__internal_carry = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 3; ++__Vi0) {
        vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(3);
    }
    for (int __Vi0 = 0; __Vi0 < 3; ++__Vi0) {
        vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(1);
    }
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(1);
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    vlSelf->Top__DOT__alu_unit__DOT__selAdder__DOT__t_Cin = VL_RAND_RESET_I(32);
    for (int __Vi0 = 0; __Vi0 < 1024; ++__Vi0) {
        vlSelf->Top__DOT__dmem_unit__DOT__mem[__Vi0] = VL_RAND_RESET_I(32);
    }
    for (int __Vi0 = 0; __Vi0 < 3; ++__Vi0) {
        vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(2);
    }
    for (int __Vi0 = 0; __Vi0 < 3; ++__Vi0) {
        vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(32);
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    vlSelf->__VdfgTmp_h2ee91c3a__0 = 0;
    vlSelf->__Vtask_ebreak__0__Vfuncout = 0;
    vlSelf->__Vtrigrprev__TOP__clk = VL_RAND_RESET_I(1);
    vlSelf->__Vtrigrprev__TOP__rst = VL_RAND_RESET_I(1);
}
