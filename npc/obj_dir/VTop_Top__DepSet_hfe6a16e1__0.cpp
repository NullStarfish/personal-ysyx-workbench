// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop_Top.h"
#include "VTop__Syms.h"

void VTop___024unit____Vdpiimwrap_ebreak_TOP____024unit();

VL_INLINE_OPT void VTop_Top___ico_sequent__TOP__Top__0(VTop_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      VTop_Top___ico_sequent__TOP__Top__0\n"); );
    // Body
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = ((IData)(4U) + vlSelf->pc_out);
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = vlSelf->pc_out;
    vlSelf->__VdfgTmp_hff5a8388__0 = ((0x80000000U 
                                       <= vlSelf->pc_out) 
                                      & (0x8000U > 
                                         ((vlSelf->pc_out 
                                           - (IData)(0x80000000U)) 
                                          >> 2U)));
    vlSelf->__VdfgTmp_hb5448552__0 = vlSelf->__PVT__imem_unit__DOT__rom0__DOT__rom_data
        [(0x7fffU & ((vlSelf->pc_out - (IData)(0x80000000U)) 
                     >> 2U))];
    if (vlSelf->__VdfgTmp_hff5a8388__0) {
        vlSelf->inst = vlSelf->__VdfgTmp_hb5448552__0;
        vlSelf->__PVT__controller_unit__DOT__funct3 
            = (7U & (vlSelf->__VdfgTmp_hb5448552__0 
                     >> 0xcU));
        vlSelf->__PVT__rs2 = (0x1fU & (vlSelf->__VdfgTmp_hb5448552__0 
                                       >> 0x14U));
        vlSelf->__VdfgTmp_h0b35cf02__0 = (vlSelf->__VdfgTmp_hb5448552__0 
                                          >> 0xcU);
        vlSelf->__VdfgTmp_he0a233d6__0 = (vlSelf->__VdfgTmp_hb5448552__0 
                                          >> 0x14U);
        vlSelf->__PVT__rd = (0x1fU & (vlSelf->__VdfgTmp_hb5448552__0 
                                      >> 7U));
        vlSelf->__VdfgTmp_h0c27f707__0 = (vlSelf->__VdfgTmp_hb5448552__0 
                                          >> 0x19U);
        vlSelf->__VdfgTmp_he0e8ece3__0 = (0x3ffU & 
                                          (vlSelf->__VdfgTmp_hb5448552__0 
                                           >> 0x15U));
        vlSelf->__VdfgTmp_h0af738b7__0 = (0xffU & (vlSelf->__VdfgTmp_hb5448552__0 
                                                   >> 0xcU));
        vlSelf->__VdfgTmp_h0ea3b6ac__0 = (0xfU & (vlSelf->__VdfgTmp_hb5448552__0 
                                                  >> 8U));
        vlSelf->__VdfgTmp_h0c741b76__0 = (0x3fU & (vlSelf->__VdfgTmp_hb5448552__0 
                                                   >> 0x19U));
        vlSelf->__PVT__rs1_raw = (0x1fU & (vlSelf->__VdfgTmp_hb5448552__0 
                                           >> 0xfU));
        vlSelf->__PVT__controller_unit__DOT__opcode 
            = (0x7fU & vlSelf->__VdfgTmp_hb5448552__0);
    } else {
        vlSelf->inst = 0U;
        vlSelf->__PVT__controller_unit__DOT__funct3 = 0U;
        vlSelf->__PVT__rs2 = 0U;
        vlSelf->__VdfgTmp_h0b35cf02__0 = 0U;
        vlSelf->__VdfgTmp_he0a233d6__0 = 0U;
        vlSelf->__PVT__rd = 0U;
        vlSelf->__VdfgTmp_h0c27f707__0 = 0U;
        vlSelf->__VdfgTmp_he0e8ece3__0 = 0U;
        vlSelf->__VdfgTmp_h0af738b7__0 = 0U;
        vlSelf->__VdfgTmp_h0ea3b6ac__0 = 0U;
        vlSelf->__VdfgTmp_h0c741b76__0 = 0U;
        vlSelf->__PVT__rs1_raw = 0U;
        vlSelf->__PVT__controller_unit__DOT__opcode = 0U;
    }
    vlSelf->__VdfgTmp_h665eeada__0 = ((6U & (((IData)(vlSelf->__VdfgTmp_hff5a8388__0) 
                                              << 1U) 
                                             & (vlSelf->__VdfgTmp_hb5448552__0 
                                                >> 0x1dU))) 
                                      | ((IData)(vlSelf->__VdfgTmp_hff5a8388__0) 
                                         & (vlSelf->__VdfgTmp_hb5448552__0 
                                            >> 0xcU)));
    vlSelf->__VdfgTmp_h0b17a187__0 = ((IData)(vlSelf->__VdfgTmp_hff5a8388__0) 
                                      & (vlSelf->__VdfgTmp_hb5448552__0 
                                         >> 0x14U));
    vlSelf->__VdfgTmp_h144a3399__0 = ((IData)(vlSelf->__VdfgTmp_hff5a8388__0) 
                                      & (vlSelf->__VdfgTmp_hb5448552__0 
                                         >> 7U));
    vlSelf->imm_gen_unit__DOT____VdfgTmp_h618f95c0__0 
        = ((IData)(vlSelf->__VdfgTmp_hff5a8388__0) 
           & (vlSelf->__VdfgTmp_hb5448552__0 >> 0x1fU));
    if ((0x100073U == vlSelf->inst)) {
        VTop___024unit____Vdpiimwrap_ebreak_TOP____024unit();
    }
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list[1U] 
        = vlSelf->__VdfgTmp_h665eeada__0;
    if ((0U == (IData)(vlSelf->__PVT__rs2))) {
        vlSelf->__PVT__reg_rs2_data = 0U;
        vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[1U] = 0U;
    } else {
        vlSelf->__PVT__reg_rs2_data = vlSelf->__PVT__reg_file_unit__DOT__reg_file
            [vlSelf->__PVT__rs2];
        vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[1U] 
            = vlSelf->__PVT__reg_file_unit__DOT__reg_file
            [vlSelf->__PVT__rs2];
    }
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[5U] 
        = (vlSelf->__VdfgTmp_h0b35cf02__0 << 0xcU);
    vlSelf->__PVT__rs1_addr = (((0x37U == (IData)(vlSelf->__PVT__opcode)) 
                                | (0x17U == (IData)(vlSelf->__PVT__opcode)))
                                ? 0U : (IData)(vlSelf->__PVT__rs1_raw));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = vlSelf->__PVT__rs1_raw;
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[4U] 
        = (((- (IData)((IData)(vlSelf->imm_gen_unit__DOT____VdfgTmp_h618f95c0__0))) 
            << 0xcU) | (IData)(vlSelf->__VdfgTmp_he0a233d6__0));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[3U] 
        = (((- (IData)((IData)(vlSelf->imm_gen_unit__DOT____VdfgTmp_h618f95c0__0))) 
            << 0xcU) | (((IData)(vlSelf->__VdfgTmp_h0c27f707__0) 
                         << 5U) | (IData)(vlSelf->__PVT__rd)));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = (((- (IData)((IData)(vlSelf->imm_gen_unit__DOT____VdfgTmp_h618f95c0__0))) 
            << 0x14U) | (((IData)(vlSelf->__VdfgTmp_h0af738b7__0) 
                          << 0xcU) | (((IData)(vlSelf->__VdfgTmp_h0b17a187__0) 
                                       << 0xbU) | ((IData)(vlSelf->__VdfgTmp_he0e8ece3__0) 
                                                   << 1U))));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[2U] 
        = (((- (IData)((IData)(vlSelf->imm_gen_unit__DOT____VdfgTmp_h618f95c0__0))) 
            << 0xcU) | (((IData)(vlSelf->__VdfgTmp_h144a3399__0) 
                         << 0xbU) | (((IData)(vlSelf->__VdfgTmp_h0c741b76__0) 
                                      << 5U) | ((IData)(vlSelf->__VdfgTmp_h0ea3b6ac__0) 
                                                << 1U))));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                       == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
           == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__WBSel = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit)
                             ? (IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out)
                             : 1U);
    vlSelf->__PVT__BrUn = ((0x63U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                           & ((IData)(vlSelf->__VdfgTmp_hff5a8388__0) 
                              & (vlSelf->__VdfgTmp_hb5448552__0 
                                 >> 0xdU)));
    vlSelf->__PVT__Asel = ((0x17U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                           | ((0x6fU == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                              | (0x63U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))));
    vlSelf->controller_unit__DOT____VdfgTmp_h7acf63f3__0 
        = ((0x37U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
           | ((0x17U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
              | ((0x6fU == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                 | (0x67U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)))));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                       == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
           == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [3U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [4U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->__PVT__ImmSel = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit)
                              ? (IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out)
                              : 1U);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                       == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
           == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__ALUSel = ((IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit)
                              ? (IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out)
                              : 0U);
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = ((0U == (IData)(vlSelf->__PVT__rs1_addr))
            ? 0U : vlSelf->__PVT__reg_file_unit__DOT__reg_file
           [vlSelf->__PVT__rs1_addr]);
    vlSelf->__PVT__Bsel = ((0x13U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                           | ((3U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                              | ((0x23U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                                 | (IData)(vlSelf->controller_unit__DOT____VdfgTmp_h7acf63f3__0))));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__ImmSel) 
                       == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__ImmSel) == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__ImmSel) 
                          == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ImmSel) == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__ImmSel) 
                          == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ImmSel) == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__ImmSel) 
                          == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [3U]))) & vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ImmSel) == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__ImmSel) 
                          == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [4U]))) & vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ImmSel) == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__ImmSel) 
                          == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [5U]))) & vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [5U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ImmSel) == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [5U]));
    vlSelf->__PVT__imm_out = ((IData)(vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit)
                               ? vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out
                               : 0U);
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (((IData)(vlSelf->__PVT__ALUSel) == vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
            [0U]) & vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__ALUSel) == vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__ALUSel) == vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
               [1U]) & vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ALUSel) == vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__ALUSel) == vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
               [2U]) & vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ALUSel) == vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__alu_unit__DOT__AdderCtrl = ((IData)(vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit) 
                                               & (IData)(vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out));
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__Asel) 
                       == vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__Asel) 
                          == vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__alu_in_a = vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out;
    vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[0U] 
        = vlSelf->__PVT__imm_out;
    vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__Bsel) 
                       == vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__Bsel) 
                          == vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__alu_in_b = vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out;
    vlSelf->__PVT__alu_unit__DOT__selAdder__DOT__t_Cin 
        = (((- (IData)((IData)(vlSelf->__PVT__alu_unit__DOT__AdderCtrl))) 
            ^ vlSelf->__PVT__alu_in_b) + (IData)(vlSelf->__PVT__alu_unit__DOT__AdderCtrl));
    vlSelf->__PVT__alu_unit__DOT__internal_carry = 
        (1U & (IData)((1ULL & (((QData)((IData)(vlSelf->__PVT__alu_in_a)) 
                                + (QData)((IData)(vlSelf->__PVT__alu_unit__DOT__selAdder__DOT__t_Cin))) 
                               >> 0x20U))));
    vlSelf->__PVT__alu_unit__DOT__AdderResult = (vlSelf->__PVT__alu_in_a 
                                                 + vlSelf->__PVT__alu_unit__DOT__selAdder__DOT__t_Cin);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[4U] 
        = (0U != vlSelf->__PVT__alu_unit__DOT__AdderResult);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[5U] 
        = (1U & (~ (IData)((0U != vlSelf->__PVT__alu_unit__DOT__AdderResult))));
    vlSelf->__PVT__alu_unit__DOT__internal_overflow 
        = (((vlSelf->__PVT__alu_in_a >> 0x1fU) == (vlSelf->__PVT__alu_in_b 
                                                   >> 0x1fU)) 
           & ((vlSelf->__PVT__alu_unit__DOT__AdderResult 
               >> 0x1fU) != (vlSelf->__PVT__alu_in_a 
                             >> 0x1fU)));
    if (vlSelf->__PVT__BrUn) {
        vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[1U] 
            = (1U & (~ (IData)(vlSelf->__PVT__alu_unit__DOT__internal_carry)));
        vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[3U] 
            = (1U & (~ (IData)(vlSelf->__PVT__alu_unit__DOT__internal_carry)));
        vlSelf->__PVT__alu_br_lt = (1U & (~ (IData)(vlSelf->__PVT__alu_unit__DOT__internal_carry)));
    } else {
        vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[1U] 
            = (1U & ((IData)(vlSelf->__PVT__alu_unit__DOT__internal_overflow) 
                     ^ (vlSelf->__PVT__alu_unit__DOT__AdderResult 
                        >> 0x1fU)));
        vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[3U] 
            = (1U & ((IData)(vlSelf->__PVT__alu_unit__DOT__internal_overflow) 
                     ^ (vlSelf->__PVT__alu_unit__DOT__AdderResult 
                        >> 0x1fU)));
        vlSelf->__PVT__alu_br_lt = (1U & ((IData)(vlSelf->__PVT__alu_unit__DOT__internal_overflow) 
                                          ^ (vlSelf->__PVT__alu_unit__DOT__AdderResult 
                                             >> 0x1fU)));
    }
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[0U] 
        = ((~ (IData)(vlSelf->__PVT__alu_br_lt)) & 
           (0U != vlSelf->__PVT__alu_unit__DOT__AdderResult));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[2U] 
        = ((~ (IData)(vlSelf->__PVT__alu_br_lt)) & 
           (0U != vlSelf->__PVT__alu_unit__DOT__AdderResult));
    vlSelf->__PVT__alu_result = ((4U & (IData)(vlSelf->__PVT__ALUSel))
                                  ? ((2U & (IData)(vlSelf->__PVT__ALUSel))
                                      ? ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                          ? 0xdeadbeefU
                                          : (((IData)(vlSelf->__PVT__alu_br_lt) 
                                              << 1U) 
                                             | (1U 
                                                & (~ (IData)(
                                                             (0U 
                                                              != vlSelf->__PVT__alu_unit__DOT__AdderResult))))))
                                      : ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                          ? (vlSelf->__PVT__alu_in_a 
                                             ^ vlSelf->__PVT__alu_in_b)
                                          : (vlSelf->__PVT__alu_in_a 
                                             | vlSelf->__PVT__alu_in_b)))
                                  : ((2U & (IData)(vlSelf->__PVT__ALUSel))
                                      ? ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                          ? (vlSelf->__PVT__alu_in_a 
                                             & vlSelf->__PVT__alu_in_b)
                                          : (~ vlSelf->__PVT__alu_in_a))
                                      : vlSelf->__PVT__alu_unit__DOT__AdderResult));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
            == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
            [0U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
           == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
               == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [1U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
              == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
               == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [2U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
              == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
               == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [3U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
              == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
               == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [4U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
              == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
               == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [5U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [5U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
              == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [5U]));
    vlSelf->__PVT__controller_unit__DOT__take_branch 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           & (IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out));
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = vlSelf->__PVT__alu_result;
    vlSelf->__PVT__pc_unit__DOT__PCin = (((0x6fU == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                                          | ((0x67U 
                                              == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                                             | ((0x63U 
                                                 == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                                                & (IData)(vlSelf->__PVT__controller_unit__DOT__take_branch))))
                                          ? vlSelf->__PVT__alu_result
                                          : ((IData)(4U) 
                                             + vlSelf->pc_out));
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

VL_INLINE_OPT void VTop_Top___nba_sequent__TOP__Top__0(VTop_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      VTop_Top___nba_sequent__TOP__Top__0\n"); );
    // Init
    SData/*9:0*/ __Vdlyvdim0__dmem_unit__DOT__mem__v0;
    __Vdlyvdim0__dmem_unit__DOT__mem__v0 = 0;
    IData/*31:0*/ __Vdlyvval__dmem_unit__DOT__mem__v0;
    __Vdlyvval__dmem_unit__DOT__mem__v0 = 0;
    CData/*0:0*/ __Vdlyvset__dmem_unit__DOT__mem__v0;
    __Vdlyvset__dmem_unit__DOT__mem__v0 = 0;
    // Body
    __Vdlyvset__dmem_unit__DOT__mem__v0 = 0U;
    if ((1U & (~ (IData)(vlSymsp->TOP.rst)))) {
        if ((0x23U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
            __Vdlyvval__dmem_unit__DOT__mem__v0 = vlSelf->__PVT__reg_rs2_data;
            __Vdlyvset__dmem_unit__DOT__mem__v0 = 1U;
            __Vdlyvdim0__dmem_unit__DOT__mem__v0 = 
                (0x3ffU & (vlSelf->__PVT__alu_result 
                           >> 2U));
        }
    }
    if (vlSymsp->TOP.rst) {
        vlSelf->__PVT__dmem_rdata = 0x80000000U;
    } else if ((0x23U != (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
        vlSelf->__PVT__dmem_rdata = vlSelf->__PVT__dmem_unit__DOT__mem
            [(0x3ffU & (vlSelf->__PVT__alu_result >> 2U))];
    }
    if (__Vdlyvset__dmem_unit__DOT__mem__v0) {
        vlSelf->__PVT__dmem_unit__DOT__mem[__Vdlyvdim0__dmem_unit__DOT__mem__v0] 
            = __Vdlyvval__dmem_unit__DOT__mem__v0;
    }
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[2U] 
        = vlSelf->__PVT__dmem_rdata;
}

VL_INLINE_OPT void VTop_Top___nba_sequent__TOP__Top__1(VTop_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      VTop_Top___nba_sequent__TOP__Top__1\n"); );
    // Init
    SData/*14:0*/ __Vdlyvdim0__imem_unit__DOT__rom0__DOT__rom_data__v0;
    __Vdlyvdim0__imem_unit__DOT__rom0__DOT__rom_data__v0 = 0;
    IData/*31:0*/ __Vdlyvval__imem_unit__DOT__rom0__DOT__rom_data__v0;
    __Vdlyvval__imem_unit__DOT__rom0__DOT__rom_data__v0 = 0;
    CData/*0:0*/ __Vdlyvset__imem_unit__DOT__rom0__DOT__rom_data__v0;
    __Vdlyvset__imem_unit__DOT__rom0__DOT__rom_data__v0 = 0;
    CData/*0:0*/ __Vdlyvset__reg_file_unit__DOT__reg_file__v0;
    __Vdlyvset__reg_file_unit__DOT__reg_file__v0 = 0;
    CData/*4:0*/ __Vdlyvdim0__reg_file_unit__DOT__reg_file__v32;
    __Vdlyvdim0__reg_file_unit__DOT__reg_file__v32 = 0;
    IData/*31:0*/ __Vdlyvval__reg_file_unit__DOT__reg_file__v32;
    __Vdlyvval__reg_file_unit__DOT__reg_file__v32 = 0;
    CData/*0:0*/ __Vdlyvset__reg_file_unit__DOT__reg_file__v32;
    __Vdlyvset__reg_file_unit__DOT__reg_file__v32 = 0;
    // Body
    __Vdlyvset__reg_file_unit__DOT__reg_file__v0 = 0U;
    __Vdlyvset__reg_file_unit__DOT__reg_file__v32 = 0U;
    __Vdlyvset__imem_unit__DOT__rom0__DOT__rom_data__v0 = 0U;
    if (vlSymsp->TOP.rst) {
        __Vdlyvset__reg_file_unit__DOT__reg_file__v0 = 1U;
    }
    if (VL_UNLIKELY((((0x33U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                      | ((0x13U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                         | ((3U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                            | (IData)(vlSelf->controller_unit__DOT____VdfgTmp_h7acf63f3__0)))) 
                     & (0U != (IData)(vlSelf->__PVT__rd))))) {
        VL_WRITEF("RegFile write: time=%0t AddrD=%2# DataD=%x\n",
                  64,VL_TIME_UNITED_Q(1),-12,5,(IData)(vlSelf->__PVT__rd),
                  32,vlSelf->__PVT__wb_data);
        __Vdlyvval__reg_file_unit__DOT__reg_file__v32 
            = vlSelf->__PVT__wb_data;
        __Vdlyvset__reg_file_unit__DOT__reg_file__v32 = 1U;
        __Vdlyvdim0__reg_file_unit__DOT__reg_file__v32 
            = vlSelf->__PVT__rd;
    }
    if ((((IData)(vlSelf->__PVT__rom_we) & (0x80000000U 
                                            <= vlSelf->__PVT__rom_waddr)) 
         & (0x8000U > ((vlSelf->__PVT__rom_waddr - (IData)(0x80000000U)) 
                       >> 2U)))) {
        __Vdlyvval__imem_unit__DOT__rom0__DOT__rom_data__v0 
            = vlSelf->__PVT__rom_wdata;
        __Vdlyvset__imem_unit__DOT__rom0__DOT__rom_data__v0 = 1U;
        __Vdlyvdim0__imem_unit__DOT__rom0__DOT__rom_data__v0 
            = (0x7fffU & ((vlSelf->__PVT__rom_waddr 
                           - (IData)(0x80000000U)) 
                          >> 2U));
    }
    if (vlSymsp->TOP.rst) {
        vlSelf->pc_out = 0x80000000U;
        vlSelf->__PVT__rom_waddr = 0U;
        vlSelf->__PVT__rom_wdata = 0U;
    } else {
        vlSelf->pc_out = vlSelf->__PVT__pc_unit__DOT__PCin;
        if (vlSymsp->TOP.load_en) {
            vlSelf->__PVT__rom_waddr = vlSymsp->TOP.load_addr;
            vlSelf->__PVT__rom_wdata = vlSymsp->TOP.load_data;
        }
    }
    if (__Vdlyvset__reg_file_unit__DOT__reg_file__v0) {
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[1U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[2U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[3U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[4U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[5U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[6U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[7U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[8U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[9U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0xaU] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0xbU] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0xcU] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0xdU] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0xeU] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0xfU] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x10U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x11U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x12U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x13U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x14U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x15U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x16U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x17U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x18U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x19U] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x1aU] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x1bU] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x1cU] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x1dU] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x1eU] = 0U;
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[0x1fU] = 0U;
    }
    if (__Vdlyvset__reg_file_unit__DOT__reg_file__v32) {
        vlSelf->__PVT__reg_file_unit__DOT__reg_file[__Vdlyvdim0__reg_file_unit__DOT__reg_file__v32] 
            = __Vdlyvval__reg_file_unit__DOT__reg_file__v32;
    }
    if (__Vdlyvset__imem_unit__DOT__rom0__DOT__rom_data__v0) {
        vlSelf->__PVT__imem_unit__DOT__rom0__DOT__rom_data[__Vdlyvdim0__imem_unit__DOT__rom0__DOT__rom_data__v0] 
            = __Vdlyvval__imem_unit__DOT__rom0__DOT__rom_data__v0;
    }
    vlSelf->__PVT__rom_we = ((~ (IData)(vlSymsp->TOP.rst)) 
                             & (IData)(vlSymsp->TOP.load_en));
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = ((IData)(4U) + vlSelf->pc_out);
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = vlSelf->pc_out;
    vlSelf->__VdfgTmp_hff5a8388__0 = ((0x80000000U 
                                       <= vlSelf->pc_out) 
                                      & (0x8000U > 
                                         ((vlSelf->pc_out 
                                           - (IData)(0x80000000U)) 
                                          >> 2U)));
    vlSelf->__VdfgTmp_hb5448552__0 = vlSelf->__PVT__imem_unit__DOT__rom0__DOT__rom_data
        [(0x7fffU & ((vlSelf->pc_out - (IData)(0x80000000U)) 
                     >> 2U))];
    if (vlSelf->__VdfgTmp_hff5a8388__0) {
        vlSelf->inst = vlSelf->__VdfgTmp_hb5448552__0;
        vlSelf->__PVT__controller_unit__DOT__funct3 
            = (7U & (vlSelf->__VdfgTmp_hb5448552__0 
                     >> 0xcU));
        vlSelf->__PVT__rs2 = (0x1fU & (vlSelf->__VdfgTmp_hb5448552__0 
                                       >> 0x14U));
        vlSelf->__VdfgTmp_h0b35cf02__0 = (vlSelf->__VdfgTmp_hb5448552__0 
                                          >> 0xcU);
        vlSelf->__VdfgTmp_he0a233d6__0 = (vlSelf->__VdfgTmp_hb5448552__0 
                                          >> 0x14U);
        vlSelf->__PVT__rd = (0x1fU & (vlSelf->__VdfgTmp_hb5448552__0 
                                      >> 7U));
        vlSelf->__VdfgTmp_h0c27f707__0 = (vlSelf->__VdfgTmp_hb5448552__0 
                                          >> 0x19U);
        vlSelf->__VdfgTmp_he0e8ece3__0 = (0x3ffU & 
                                          (vlSelf->__VdfgTmp_hb5448552__0 
                                           >> 0x15U));
        vlSelf->__VdfgTmp_h0af738b7__0 = (0xffU & (vlSelf->__VdfgTmp_hb5448552__0 
                                                   >> 0xcU));
        vlSelf->__VdfgTmp_h0ea3b6ac__0 = (0xfU & (vlSelf->__VdfgTmp_hb5448552__0 
                                                  >> 8U));
        vlSelf->__VdfgTmp_h0c741b76__0 = (0x3fU & (vlSelf->__VdfgTmp_hb5448552__0 
                                                   >> 0x19U));
        vlSelf->__PVT__rs1_raw = (0x1fU & (vlSelf->__VdfgTmp_hb5448552__0 
                                           >> 0xfU));
        vlSelf->__PVT__controller_unit__DOT__opcode 
            = (0x7fU & vlSelf->__VdfgTmp_hb5448552__0);
    } else {
        vlSelf->inst = 0U;
        vlSelf->__PVT__controller_unit__DOT__funct3 = 0U;
        vlSelf->__PVT__rs2 = 0U;
        vlSelf->__VdfgTmp_h0b35cf02__0 = 0U;
        vlSelf->__VdfgTmp_he0a233d6__0 = 0U;
        vlSelf->__PVT__rd = 0U;
        vlSelf->__VdfgTmp_h0c27f707__0 = 0U;
        vlSelf->__VdfgTmp_he0e8ece3__0 = 0U;
        vlSelf->__VdfgTmp_h0af738b7__0 = 0U;
        vlSelf->__VdfgTmp_h0ea3b6ac__0 = 0U;
        vlSelf->__VdfgTmp_h0c741b76__0 = 0U;
        vlSelf->__PVT__rs1_raw = 0U;
        vlSelf->__PVT__controller_unit__DOT__opcode = 0U;
    }
    vlSelf->__VdfgTmp_h665eeada__0 = ((6U & (((IData)(vlSelf->__VdfgTmp_hff5a8388__0) 
                                              << 1U) 
                                             & (vlSelf->__VdfgTmp_hb5448552__0 
                                                >> 0x1dU))) 
                                      | ((IData)(vlSelf->__VdfgTmp_hff5a8388__0) 
                                         & (vlSelf->__VdfgTmp_hb5448552__0 
                                            >> 0xcU)));
    vlSelf->__VdfgTmp_h0b17a187__0 = ((IData)(vlSelf->__VdfgTmp_hff5a8388__0) 
                                      & (vlSelf->__VdfgTmp_hb5448552__0 
                                         >> 0x14U));
    vlSelf->__VdfgTmp_h144a3399__0 = ((IData)(vlSelf->__VdfgTmp_hff5a8388__0) 
                                      & (vlSelf->__VdfgTmp_hb5448552__0 
                                         >> 7U));
    vlSelf->imm_gen_unit__DOT____VdfgTmp_h618f95c0__0 
        = ((IData)(vlSelf->__VdfgTmp_hff5a8388__0) 
           & (vlSelf->__VdfgTmp_hb5448552__0 >> 0x1fU));
    if ((0x100073U == vlSelf->inst)) {
        VTop___024unit____Vdpiimwrap_ebreak_TOP____024unit();
    }
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list[1U] 
        = vlSelf->__VdfgTmp_h665eeada__0;
    if ((0U == (IData)(vlSelf->__PVT__rs2))) {
        vlSelf->__PVT__reg_rs2_data = 0U;
        vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[1U] = 0U;
    } else {
        vlSelf->__PVT__reg_rs2_data = vlSelf->__PVT__reg_file_unit__DOT__reg_file
            [vlSelf->__PVT__rs2];
        vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[1U] 
            = vlSelf->__PVT__reg_file_unit__DOT__reg_file
            [vlSelf->__PVT__rs2];
    }
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[5U] 
        = (vlSelf->__VdfgTmp_h0b35cf02__0 << 0xcU);
    vlSelf->__PVT__rs1_addr = (((0x37U == (IData)(vlSelf->__PVT__opcode)) 
                                | (0x17U == (IData)(vlSelf->__PVT__opcode)))
                                ? 0U : (IData)(vlSelf->__PVT__rs1_raw));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = vlSelf->__PVT__rs1_raw;
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[4U] 
        = (((- (IData)((IData)(vlSelf->imm_gen_unit__DOT____VdfgTmp_h618f95c0__0))) 
            << 0xcU) | (IData)(vlSelf->__VdfgTmp_he0a233d6__0));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[3U] 
        = (((- (IData)((IData)(vlSelf->imm_gen_unit__DOT____VdfgTmp_h618f95c0__0))) 
            << 0xcU) | (((IData)(vlSelf->__VdfgTmp_h0c27f707__0) 
                         << 5U) | (IData)(vlSelf->__PVT__rd)));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = (((- (IData)((IData)(vlSelf->imm_gen_unit__DOT____VdfgTmp_h618f95c0__0))) 
            << 0x14U) | (((IData)(vlSelf->__VdfgTmp_h0af738b7__0) 
                          << 0xcU) | (((IData)(vlSelf->__VdfgTmp_h0b17a187__0) 
                                       << 0xbU) | ((IData)(vlSelf->__VdfgTmp_he0e8ece3__0) 
                                                   << 1U))));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[2U] 
        = (((- (IData)((IData)(vlSelf->imm_gen_unit__DOT____VdfgTmp_h618f95c0__0))) 
            << 0xcU) | (((IData)(vlSelf->__VdfgTmp_h144a3399__0) 
                         << 0xbU) | (((IData)(vlSelf->__VdfgTmp_h0c741b76__0) 
                                      << 5U) | ((IData)(vlSelf->__VdfgTmp_h0ea3b6ac__0) 
                                                << 1U))));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                       == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
           == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__WBSel = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit)
                             ? (IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out)
                             : 1U);
    vlSelf->__PVT__BrUn = ((0x63U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                           & ((IData)(vlSelf->__VdfgTmp_hff5a8388__0) 
                              & (vlSelf->__VdfgTmp_hb5448552__0 
                                 >> 0xdU)));
    vlSelf->__PVT__Asel = ((0x17U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                           | ((0x6fU == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                              | (0x63U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))));
    vlSelf->controller_unit__DOT____VdfgTmp_h7acf63f3__0 
        = ((0x37U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
           | ((0x17U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
              | ((0x6fU == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                 | (0x67U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)))));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                       == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
           == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [3U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [4U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->__PVT__ImmSel = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit)
                              ? (IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out)
                              : 1U);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                       == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
           == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
              == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__ALUSel = ((IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit)
                              ? (IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out)
                              : 0U);
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = ((0U == (IData)(vlSelf->__PVT__rs1_addr))
            ? 0U : vlSelf->__PVT__reg_file_unit__DOT__reg_file
           [vlSelf->__PVT__rs1_addr]);
    vlSelf->__PVT__Bsel = ((0x13U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                           | ((3U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                              | ((0x23U == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                                 | (IData)(vlSelf->controller_unit__DOT____VdfgTmp_h7acf63f3__0))));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__ImmSel) 
                       == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__ImmSel) == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__ImmSel) 
                          == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ImmSel) == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__ImmSel) 
                          == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ImmSel) == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__ImmSel) 
                          == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [3U]))) & vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ImmSel) == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__ImmSel) 
                          == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [4U]))) & vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ImmSel) == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__ImmSel) 
                          == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [5U]))) & vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [5U]));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ImmSel) == vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [5U]));
    vlSelf->__PVT__imm_out = ((IData)(vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit)
                               ? vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out
                               : 0U);
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (((IData)(vlSelf->__PVT__ALUSel) == vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
            [0U]) & vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__ALUSel) == vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__ALUSel) == vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
               [1U]) & vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ALUSel) == vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__ALUSel) == vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
               [2U]) & vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__ALUSel) == vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__alu_unit__DOT__AdderCtrl = ((IData)(vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit) 
                                               & (IData)(vlSelf->__PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out));
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__Asel) 
                       == vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__Asel) 
                          == vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__alu_in_a = vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out;
    vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[0U] 
        = vlSelf->__PVT__imm_out;
    vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->__PVT__Bsel) 
                       == vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out 
        = (vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->__PVT__Bsel) 
                          == vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__alu_in_b = vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out;
    vlSelf->__PVT__alu_unit__DOT__selAdder__DOT__t_Cin 
        = (((- (IData)((IData)(vlSelf->__PVT__alu_unit__DOT__AdderCtrl))) 
            ^ vlSelf->__PVT__alu_in_b) + (IData)(vlSelf->__PVT__alu_unit__DOT__AdderCtrl));
    vlSelf->__PVT__alu_unit__DOT__internal_carry = 
        (1U & (IData)((1ULL & (((QData)((IData)(vlSelf->__PVT__alu_in_a)) 
                                + (QData)((IData)(vlSelf->__PVT__alu_unit__DOT__selAdder__DOT__t_Cin))) 
                               >> 0x20U))));
    vlSelf->__PVT__alu_unit__DOT__AdderResult = (vlSelf->__PVT__alu_in_a 
                                                 + vlSelf->__PVT__alu_unit__DOT__selAdder__DOT__t_Cin);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[4U] 
        = (0U != vlSelf->__PVT__alu_unit__DOT__AdderResult);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[5U] 
        = (1U & (~ (IData)((0U != vlSelf->__PVT__alu_unit__DOT__AdderResult))));
    vlSelf->__PVT__alu_unit__DOT__internal_overflow 
        = (((vlSelf->__PVT__alu_in_a >> 0x1fU) == (vlSelf->__PVT__alu_in_b 
                                                   >> 0x1fU)) 
           & ((vlSelf->__PVT__alu_unit__DOT__AdderResult 
               >> 0x1fU) != (vlSelf->__PVT__alu_in_a 
                             >> 0x1fU)));
    if (vlSelf->__PVT__BrUn) {
        vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[1U] 
            = (1U & (~ (IData)(vlSelf->__PVT__alu_unit__DOT__internal_carry)));
        vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[3U] 
            = (1U & (~ (IData)(vlSelf->__PVT__alu_unit__DOT__internal_carry)));
        vlSelf->__PVT__alu_br_lt = (1U & (~ (IData)(vlSelf->__PVT__alu_unit__DOT__internal_carry)));
    } else {
        vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[1U] 
            = (1U & ((IData)(vlSelf->__PVT__alu_unit__DOT__internal_overflow) 
                     ^ (vlSelf->__PVT__alu_unit__DOT__AdderResult 
                        >> 0x1fU)));
        vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[3U] 
            = (1U & ((IData)(vlSelf->__PVT__alu_unit__DOT__internal_overflow) 
                     ^ (vlSelf->__PVT__alu_unit__DOT__AdderResult 
                        >> 0x1fU)));
        vlSelf->__PVT__alu_br_lt = (1U & ((IData)(vlSelf->__PVT__alu_unit__DOT__internal_overflow) 
                                          ^ (vlSelf->__PVT__alu_unit__DOT__AdderResult 
                                             >> 0x1fU)));
    }
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[0U] 
        = ((~ (IData)(vlSelf->__PVT__alu_br_lt)) & 
           (0U != vlSelf->__PVT__alu_unit__DOT__AdderResult));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[2U] 
        = ((~ (IData)(vlSelf->__PVT__alu_br_lt)) & 
           (0U != vlSelf->__PVT__alu_unit__DOT__AdderResult));
    vlSelf->__PVT__alu_result = ((4U & (IData)(vlSelf->__PVT__ALUSel))
                                  ? ((2U & (IData)(vlSelf->__PVT__ALUSel))
                                      ? ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                          ? 0xdeadbeefU
                                          : (((IData)(vlSelf->__PVT__alu_br_lt) 
                                              << 1U) 
                                             | (1U 
                                                & (~ (IData)(
                                                             (0U 
                                                              != vlSelf->__PVT__alu_unit__DOT__AdderResult))))))
                                      : ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                          ? (vlSelf->__PVT__alu_in_a 
                                             ^ vlSelf->__PVT__alu_in_b)
                                          : (vlSelf->__PVT__alu_in_a 
                                             | vlSelf->__PVT__alu_in_b)))
                                  : ((2U & (IData)(vlSelf->__PVT__ALUSel))
                                      ? ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                          ? (vlSelf->__PVT__alu_in_a 
                                             & vlSelf->__PVT__alu_in_b)
                                          : (~ vlSelf->__PVT__alu_in_a))
                                      : vlSelf->__PVT__alu_unit__DOT__AdderResult));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
            == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
            [0U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
           == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
               == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [1U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
              == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
               == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [2U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
              == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
               == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [3U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
              == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
               == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [4U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
              == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
               == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [5U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [5U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
              == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [5U]));
    vlSelf->__PVT__controller_unit__DOT__take_branch 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           & (IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out));
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = vlSelf->__PVT__alu_result;
    vlSelf->__PVT__pc_unit__DOT__PCin = (((0x6fU == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                                          | ((0x67U 
                                              == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                                             | ((0x63U 
                                                 == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                                                & (IData)(vlSelf->__PVT__controller_unit__DOT__take_branch))))
                                          ? vlSelf->__PVT__alu_result
                                          : ((IData)(4U) 
                                             + vlSelf->pc_out));
}
