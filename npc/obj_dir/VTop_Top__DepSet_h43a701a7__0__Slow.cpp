// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop_Top.h"

VL_ATTR_COLD void VTop_Top___eval_initial__TOP__Top(VTop_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      VTop_Top___eval_initial__TOP__Top\n"); );
    // Body
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list[0U] = 0x63U;
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list[1U] = 0x33U;
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list[0U] = 6U;
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[0U] = 7U;
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[1U] = 6U;
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[2U] = 5U;
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[3U] = 4U;
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[4U] = 1U;
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[5U] = 0U;
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[0U] = 5U;
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[1U] = 4U;
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[2U] = 3U;
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[3U] = 2U;
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[4U] = 1U;
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[5U] = 0U;
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list[0U] = 1U;
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list[1U] = 0U;
    vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list[0U] = 1U;
    vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list[1U] = 0U;
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list[0U] = 2U;
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list[1U] = 1U;
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list[2U] = 0U;
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list[0U] = 0x67U;
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list[1U] = 0x6fU;
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list[2U] = 3U;
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list[0U] = 2U;
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list[1U] = 2U;
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list[2U] = 0U;
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list[0U] = 0x6fU;
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list[1U] = 0x63U;
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list[2U] = 0x23U;
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list[3U] = 0x17U;
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list[4U] = 0x37U;
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list[0U] = 4U;
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list[1U] = 3U;
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list[2U] = 2U;
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list[3U] = 0U;
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list[4U] = 0U;
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list[0U] = 6U;
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list[1U] = 1U;
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list[2U] = 0U;
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__data_list[0U] = 1U;
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__data_list[1U] = 1U;
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__data_list[2U] = 0U;
}

VL_ATTR_COLD void VTop_Top___ctor_var_reset(VTop_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      VTop_Top___ctor_var_reset\n"); );
    // Body
    vlSelf->clk = VL_RAND_RESET_I(1);
    vlSelf->rst = VL_RAND_RESET_I(1);
    vlSelf->load_en = VL_RAND_RESET_I(1);
    vlSelf->load_addr = VL_RAND_RESET_I(32);
    vlSelf->load_data = VL_RAND_RESET_I(32);
    vlSelf->pc_out = VL_RAND_RESET_I(32);
    vlSelf->inst = VL_RAND_RESET_I(32);
    vlSelf->__PVT__opcode = VL_RAND_RESET_I(7);
    vlSelf->__PVT__rd = VL_RAND_RESET_I(5);
    vlSelf->__PVT__rs1_raw = VL_RAND_RESET_I(5);
    vlSelf->__PVT__rs2 = VL_RAND_RESET_I(5);
    vlSelf->__PVT__Asel = VL_RAND_RESET_I(1);
    vlSelf->__PVT__Bsel = VL_RAND_RESET_I(1);
    vlSelf->__PVT__BrUn = VL_RAND_RESET_I(1);
    vlSelf->__PVT__WBSel = VL_RAND_RESET_I(2);
    vlSelf->__PVT__ImmSel = VL_RAND_RESET_I(3);
    vlSelf->__PVT__ALUSel = VL_RAND_RESET_I(3);
    vlSelf->__PVT__rs1_addr = VL_RAND_RESET_I(5);
    vlSelf->__PVT__reg_rs2_data = VL_RAND_RESET_I(32);
    vlSelf->__PVT__wb_data = VL_RAND_RESET_I(32);
    vlSelf->__PVT__imm_out = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_in_a = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_in_b = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_result = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_br_lt = VL_RAND_RESET_I(1);
    vlSelf->__PVT__dmem_rdata = VL_RAND_RESET_I(32);
    vlSelf->__PVT__rom_we = VL_RAND_RESET_I(1);
    vlSelf->__PVT__rom_waddr = VL_RAND_RESET_I(32);
    vlSelf->__PVT__rom_wdata = VL_RAND_RESET_I(32);
    vlSelf->__PVT__pc_unit__DOT__PCin = VL_RAND_RESET_I(32);
    for (int __Vi0 = 0; __Vi0 < 32768; ++__Vi0) {
        vlSelf->__PVT__imem_unit__DOT__rom0__DOT__rom_data[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->__PVT__controller_unit__DOT__opcode = VL_RAND_RESET_I(7);
    vlSelf->__PVT__controller_unit__DOT__funct3 = VL_RAND_RESET_I(3);
    vlSelf->__PVT__controller_unit__DOT__take_branch = VL_RAND_RESET_I(1);
    vlSelf->controller_unit__DOT____VdfgTmp_h7acf63f3__0 = 0;
    for (int __Vi0 = 0; __Vi0 < 3; ++__Vi0) {
        vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(7);
    }
    for (int __Vi0 = 0; __Vi0 < 3; ++__Vi0) {
        vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(2);
    }
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(2);
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 5; ++__Vi0) {
        vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(7);
    }
    for (int __Vi0 = 0; __Vi0 < 5; ++__Vi0) {
        vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(3);
    }
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(3);
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 2; ++__Vi0) {
        vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(7);
    }
    for (int __Vi0 = 0; __Vi0 < 2; ++__Vi0) {
        vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(3);
    }
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(3);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 6; ++__Vi0) {
        vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(3);
    }
    for (int __Vi0 = 0; __Vi0 < 6; ++__Vi0) {
        vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(1);
    }
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(1);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 32; ++__Vi0) {
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->imm_gen_unit__DOT____VdfgTmp_h618f95c0__0 = 0;
    for (int __Vi0 = 0; __Vi0 < 6; ++__Vi0) {
        vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(3);
    }
    for (int __Vi0 = 0; __Vi0 < 6; ++__Vi0) {
        vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(32);
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 2; ++__Vi0) {
        vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(1);
    }
    for (int __Vi0 = 0; __Vi0 < 2; ++__Vi0) {
        vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(32);
    for (int __Vi0 = 0; __Vi0 < 2; ++__Vi0) {
        vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(1);
    }
    for (int __Vi0 = 0; __Vi0 < 2; ++__Vi0) {
        vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_unit__DOT__AdderCtrl = VL_RAND_RESET_I(1);
    vlSelf->__PVT__alu_unit__DOT__AdderResult = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_unit__DOT__internal_overflow = VL_RAND_RESET_I(1);
    vlSelf->__PVT__alu_unit__DOT__internal_carry = VL_RAND_RESET_I(1);
    for (int __Vi0 = 0; __Vi0 < 3; ++__Vi0) {
        vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(3);
    }
    for (int __Vi0 = 0; __Vi0 < 3; ++__Vi0) {
        vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(1);
    }
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(1);
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    vlSelf->__PVT__alu_unit__DOT__selAdder__DOT__t_Cin = VL_RAND_RESET_I(32);
    for (int __Vi0 = 0; __Vi0 < 1024; ++__Vi0) {
        vlSelf->__PVT__dmem_unit__DOT__mem[__Vi0] = VL_RAND_RESET_I(32);
    }
    for (int __Vi0 = 0; __Vi0 < 3; ++__Vi0) {
        vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list[__Vi0] = VL_RAND_RESET_I(2);
    }
    for (int __Vi0 = 0; __Vi0 < 3; ++__Vi0) {
        vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out = VL_RAND_RESET_I(32);
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit = VL_RAND_RESET_I(1);
    vlSelf->__VdfgTmp_hff5a8388__0 = 0;
    vlSelf->__VdfgTmp_hb5448552__0 = 0;
    vlSelf->__VdfgTmp_h665eeada__0 = 0;
    vlSelf->__VdfgTmp_h0b35cf02__0 = 0;
    vlSelf->__VdfgTmp_he0a233d6__0 = 0;
    vlSelf->__VdfgTmp_h0c27f707__0 = 0;
    vlSelf->__VdfgTmp_h144a3399__0 = 0;
    vlSelf->__VdfgTmp_h0c741b76__0 = 0;
    vlSelf->__VdfgTmp_h0ea3b6ac__0 = 0;
    vlSelf->__VdfgTmp_h0af738b7__0 = 0;
    vlSelf->__VdfgTmp_h0b17a187__0 = 0;
    vlSelf->__VdfgTmp_he0e8ece3__0 = 0;
}
