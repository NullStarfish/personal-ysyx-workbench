// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub_Top.h"

VL_ATTR_COLD void Vaddandsub_Top___eval_initial__TOP__Top(Vaddandsub_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      Vaddandsub_Top___eval_initial__TOP__Top\n"); );
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

VL_ATTR_COLD void Vaddandsub_Top___stl_sequent__TOP__Top__0(Vaddandsub_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      Vaddandsub_Top___stl_sequent__TOP__Top__0\n"); );
    // Body
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = ((IData)(4U) + vlSelf->__PVT__pc_out);
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[2U] 
        = vlSelf->__PVT__dmem_rdata;
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = vlSelf->__PVT__pc_out;
}

VL_ATTR_COLD void Vaddandsub_Top___ctor_var_reset(Vaddandsub_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      Vaddandsub_Top___ctor_var_reset\n"); );
    // Body
    vlSelf->clk = VL_RAND_RESET_I(1);
    vlSelf->rst = VL_RAND_RESET_I(1);
    vlSelf->__PVT__pc_out = VL_RAND_RESET_I(32);
    vlSelf->__PVT__opcode = VL_RAND_RESET_I(7);
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
    vlSelf->__PVT__pc_unit__DOT__PCin = VL_RAND_RESET_I(32);
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
    vlSelf->__VdfgTmp_h9f39adc8__0 = 0;
    vlSelf->__Vtask_ebreak__0__Vfuncout = 0;
}
