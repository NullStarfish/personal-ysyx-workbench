// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop__Syms.h"
#include "VTop___024root.h"

#ifdef VL_DEBUG
VL_ATTR_COLD void VTop___024root___dump_triggers__stl(VTop___024root* vlSelf);
#endif  // VL_DEBUG

VL_ATTR_COLD void VTop___024root___eval_triggers__stl(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___eval_triggers__stl\n"); );
    // Body
    vlSelf->__VstlTriggered.at(0U) = (0U == vlSelf->__VstlIterCount);
#ifdef VL_DEBUG
    if (VL_UNLIKELY(vlSymsp->_vm_contextp__->debug())) {
        VTop___024root___dump_triggers__stl(vlSelf);
    }
#endif
}

void VTop___024unit____Vdpiimwrap_ebreak_TOP____024unit(IData/*31:0*/ &ebreak__Vfuncrtn);

VL_ATTR_COLD void VTop___024root___stl_sequent__TOP__0(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___stl_sequent__TOP__0\n"); );
    // Body
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = ((IData)(4U) + vlSelf->Top__DOT__pc_out);
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[2U] 
        = vlSelf->Top__DOT__dmem_rdata;
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = vlSelf->Top__DOT__pc_out;
    vlSelf->Top__DOT__inst = vlSelf->Top__DOT__imem_unit__DOT__rom0__DOT__rom_data
        [(0xffU & (vlSelf->Top__DOT__pc_out >> 2U))];
    if ((0x100073U == vlSelf->Top__DOT__inst)) {
        VTop___024unit____Vdpiimwrap_ebreak_TOP____024unit(vlSelf->__Vtask_ebreak__0__Vfuncout);
    }
    if ((0U == (0x1fU & (vlSelf->Top__DOT__inst >> 0x14U)))) {
        vlSelf->Top__DOT__reg_rs2_data = 0U;
        vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[1U] = 0U;
    } else {
        vlSelf->Top__DOT__reg_rs2_data = vlSelf->Top__DOT__reg_file_unit__DOT__reg_file
            [(0x1fU & (vlSelf->Top__DOT__inst >> 0x14U))];
        vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[1U] 
            = vlSelf->Top__DOT__reg_file_unit__DOT__reg_file
            [(0x1fU & (vlSelf->Top__DOT__inst >> 0x14U))];
    }
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((0x7fU & vlSelf->Top__DOT__inst) 
                       == vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((0x7fU & vlSelf->Top__DOT__inst) == vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSelf->Top__DOT__inst) 
                          == vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSelf->Top__DOT__inst) == vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSelf->Top__DOT__inst) 
                          == vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSelf->Top__DOT__inst) == vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->Top__DOT__WBSel = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit)
                                ? (IData)(vlSelf->Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out)
                                : 1U);
    vlSelf->Top__DOT__BrUn = (IData)((0x2063U == (0x207fU 
                                                  & vlSelf->Top__DOT__inst)));
    vlSelf->__VdfgTmp_h2ee91c3a__0 = ((2U & (vlSelf->Top__DOT__inst 
                                             >> 0x1dU)) 
                                      | (1U & (vlSelf->Top__DOT__inst 
                                               >> 0xcU)));
    vlSelf->Top__DOT__Asel = ((0x17U == (0x7fU & vlSelf->Top__DOT__inst)) 
                              | ((0x6fU == (0x7fU & vlSelf->Top__DOT__inst)) 
                                 | (0x63U == (0x7fU 
                                              & vlSelf->Top__DOT__inst))));
    vlSelf->Top__DOT__rs1_addr = (((0x37U == (IData)(vlSelf->Top__DOT__opcode)) 
                                   | (0x17U == (IData)(vlSelf->Top__DOT__opcode)))
                                   ? 0U : (0x1fU & 
                                           (vlSelf->Top__DOT__inst 
                                            >> 0xfU)));
    vlSelf->Top__DOT__controller_unit__DOT____VdfgTmp_h7acf63f3__0 
        = ((0x37U == (0x7fU & vlSelf->Top__DOT__inst)) 
           | ((0x17U == (0x7fU & vlSelf->Top__DOT__inst)) 
              | ((0x6fU == (0x7fU & vlSelf->Top__DOT__inst)) 
                 | (0x67U == (0x7fU & vlSelf->Top__DOT__inst)))));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = (0x1fU & (vlSelf->Top__DOT__inst >> 0xfU));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = (((- (IData)((vlSelf->Top__DOT__inst >> 0x1fU))) 
            << 0x14U) | ((0xff000U & vlSelf->Top__DOT__inst) 
                         | ((0x800U & (vlSelf->Top__DOT__inst 
                                       >> 9U)) | (0x7feU 
                                                  & (vlSelf->Top__DOT__inst 
                                                     >> 0x14U)))));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[2U] 
        = (((- (IData)((vlSelf->Top__DOT__inst >> 0x1fU))) 
            << 0xcU) | ((0x800U & (vlSelf->Top__DOT__inst 
                                   << 4U)) | ((0x7e0U 
                                               & (vlSelf->Top__DOT__inst 
                                                  >> 0x14U)) 
                                              | (0x1eU 
                                                 & (vlSelf->Top__DOT__inst 
                                                    >> 7U)))));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[3U] 
        = (((- (IData)((vlSelf->Top__DOT__inst >> 0x1fU))) 
            << 0xcU) | ((0xfe0U & (vlSelf->Top__DOT__inst 
                                   >> 0x14U)) | (0x1fU 
                                                 & (vlSelf->Top__DOT__inst 
                                                    >> 7U))));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[4U] 
        = (((- (IData)((vlSelf->Top__DOT__inst >> 0x1fU))) 
            << 0xcU) | (vlSelf->Top__DOT__inst >> 0x14U));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[5U] 
        = (0xfffff000U & vlSelf->Top__DOT__inst);
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((0x7fU & vlSelf->Top__DOT__inst) 
                       == vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((0x7fU & vlSelf->Top__DOT__inst) == vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSelf->Top__DOT__inst) 
                          == vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSelf->Top__DOT__inst) == vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSelf->Top__DOT__inst) 
                          == vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSelf->Top__DOT__inst) == vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSelf->Top__DOT__inst) 
                          == vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [3U]))) & vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSelf->Top__DOT__inst) == vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSelf->Top__DOT__inst) 
                          == vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [4U]))) & vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSelf->Top__DOT__inst) == vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->Top__DOT__ImmSel = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit)
                                 ? (IData)(vlSelf->Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out)
                                 : 1U);
    vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list[1U] 
        = vlSelf->__VdfgTmp_h2ee91c3a__0;
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = ((0U == (IData)(vlSelf->Top__DOT__rs1_addr))
            ? 0U : vlSelf->Top__DOT__reg_file_unit__DOT__reg_file
           [vlSelf->Top__DOT__rs1_addr]);
    vlSelf->Top__DOT__Bsel = ((0x13U == (0x7fU & vlSelf->Top__DOT__inst)) 
                              | ((3U == (0x7fU & vlSelf->Top__DOT__inst)) 
                                 | ((0x23U == (0x7fU 
                                               & vlSelf->Top__DOT__inst)) 
                                    | (IData)(vlSelf->Top__DOT__controller_unit__DOT____VdfgTmp_h7acf63f3__0))));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->Top__DOT__ImmSel) 
                       == vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__ImmSel) == vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->Top__DOT__ImmSel) 
                          == vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->Top__DOT__ImmSel) == 
              vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->Top__DOT__ImmSel) 
                          == vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->Top__DOT__ImmSel) == 
              vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->Top__DOT__ImmSel) 
                          == vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [3U]))) & vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->Top__DOT__ImmSel) == 
              vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->Top__DOT__ImmSel) 
                          == vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [4U]))) & vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->Top__DOT__ImmSel) == 
              vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->Top__DOT__ImmSel) 
                          == vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
                          [5U]))) & vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list
              [5U]));
    vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->Top__DOT__ImmSel) == 
              vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list
              [5U]));
    vlSelf->Top__DOT__imm_out = ((IData)(vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit)
                                  ? vlSelf->Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out
                                  : 0U);
    vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((0x7fU & vlSelf->Top__DOT__inst) 
                       == vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit 
        = ((0x7fU & vlSelf->Top__DOT__inst) == vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSelf->Top__DOT__inst) 
                          == vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSelf->Top__DOT__inst) == vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->Top__DOT__ALUSel = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit)
                                 ? (IData)(vlSelf->Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out)
                                 : 0U);
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->Top__DOT__Asel) 
                       == vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->Top__DOT__Asel) 
                          == vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->Top__DOT__alu_in_a = vlSelf->Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out;
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[0U] 
        = vlSelf->Top__DOT__imm_out;
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = (((IData)(vlSelf->Top__DOT__ALUSel) == vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
            [0U]) & vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__ALUSel) == vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->Top__DOT__ALUSel) == 
               vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
               [1U]) & vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->Top__DOT__ALUSel) == 
              vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out) 
           | (((IData)(vlSelf->Top__DOT__ALUSel) == 
               vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
               [2U]) & vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__hit) 
           | ((IData)(vlSelf->Top__DOT__ALUSel) == 
              vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->Top__DOT__alu_unit__DOT__AdderCtrl = ((IData)(vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__hit) 
                                                  & (IData)(vlSelf->Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out));
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out 
        = ((- (IData)(((IData)(vlSelf->Top__DOT__Bsel) 
                       == vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out 
        = (vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out 
           | ((- (IData)(((IData)(vlSelf->Top__DOT__Bsel) 
                          == vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->Top__DOT__alu_in_b = vlSelf->Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out;
    vlSelf->Top__DOT__alu_unit__DOT__selAdder__DOT__t_Cin 
        = (((- (IData)((IData)(vlSelf->Top__DOT__alu_unit__DOT__AdderCtrl))) 
            ^ vlSelf->Top__DOT__alu_in_b) + (IData)(vlSelf->Top__DOT__alu_unit__DOT__AdderCtrl));
    vlSelf->Top__DOT__alu_unit__DOT__internal_carry 
        = (1U & (IData)((1ULL & (((QData)((IData)(vlSelf->Top__DOT__alu_in_a)) 
                                  + (QData)((IData)(vlSelf->Top__DOT__alu_unit__DOT__selAdder__DOT__t_Cin))) 
                                 >> 0x20U))));
    vlSelf->Top__DOT__alu_unit__DOT__AdderResult = 
        (vlSelf->Top__DOT__alu_in_a + vlSelf->Top__DOT__alu_unit__DOT__selAdder__DOT__t_Cin);
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[4U] 
        = (0U != vlSelf->Top__DOT__alu_unit__DOT__AdderResult);
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[5U] 
        = (1U & (~ (IData)((0U != vlSelf->Top__DOT__alu_unit__DOT__AdderResult))));
    vlSelf->Top__DOT__alu_unit__DOT__internal_overflow 
        = (((vlSelf->Top__DOT__alu_in_a >> 0x1fU) == 
            (vlSelf->Top__DOT__alu_in_b >> 0x1fU)) 
           & ((vlSelf->Top__DOT__alu_unit__DOT__AdderResult 
               >> 0x1fU) != (vlSelf->Top__DOT__alu_in_a 
                             >> 0x1fU)));
    if (vlSelf->Top__DOT__BrUn) {
        vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[1U] 
            = (1U & (~ (IData)(vlSelf->Top__DOT__alu_unit__DOT__internal_carry)));
        vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[3U] 
            = (1U & (~ (IData)(vlSelf->Top__DOT__alu_unit__DOT__internal_carry)));
        vlSelf->Top__DOT__alu_br_lt = (1U & (~ (IData)(vlSelf->Top__DOT__alu_unit__DOT__internal_carry)));
    } else {
        vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[1U] 
            = (1U & ((IData)(vlSelf->Top__DOT__alu_unit__DOT__internal_overflow) 
                     ^ (vlSelf->Top__DOT__alu_unit__DOT__AdderResult 
                        >> 0x1fU)));
        vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[3U] 
            = (1U & ((IData)(vlSelf->Top__DOT__alu_unit__DOT__internal_overflow) 
                     ^ (vlSelf->Top__DOT__alu_unit__DOT__AdderResult 
                        >> 0x1fU)));
        vlSelf->Top__DOT__alu_br_lt = (1U & ((IData)(vlSelf->Top__DOT__alu_unit__DOT__internal_overflow) 
                                             ^ (vlSelf->Top__DOT__alu_unit__DOT__AdderResult 
                                                >> 0x1fU)));
    }
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[0U] 
        = ((~ (IData)(vlSelf->Top__DOT__alu_br_lt)) 
           & (0U != vlSelf->Top__DOT__alu_unit__DOT__AdderResult));
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list[2U] 
        = ((~ (IData)(vlSelf->Top__DOT__alu_br_lt)) 
           & (0U != vlSelf->Top__DOT__alu_unit__DOT__AdderResult));
    vlSelf->Top__DOT__alu_result = ((4U & (IData)(vlSelf->Top__DOT__ALUSel))
                                     ? ((2U & (IData)(vlSelf->Top__DOT__ALUSel))
                                         ? ((1U & (IData)(vlSelf->Top__DOT__ALUSel))
                                             ? 0xdeadbeefU
                                             : (((IData)(vlSelf->Top__DOT__alu_br_lt) 
                                                 << 1U) 
                                                | (1U 
                                                   & (~ (IData)(
                                                                (0U 
                                                                 != vlSelf->Top__DOT__alu_unit__DOT__AdderResult))))))
                                         : ((1U & (IData)(vlSelf->Top__DOT__ALUSel))
                                             ? (vlSelf->Top__DOT__alu_in_a 
                                                ^ vlSelf->Top__DOT__alu_in_b)
                                             : (vlSelf->Top__DOT__alu_in_a 
                                                | vlSelf->Top__DOT__alu_in_b)))
                                     : ((2U & (IData)(vlSelf->Top__DOT__ALUSel))
                                         ? ((1U & (IData)(vlSelf->Top__DOT__ALUSel))
                                             ? (vlSelf->Top__DOT__alu_in_a 
                                                & vlSelf->Top__DOT__alu_in_b)
                                             : (~ vlSelf->Top__DOT__alu_in_a))
                                         : vlSelf->Top__DOT__alu_unit__DOT__AdderResult));
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = (((7U & (vlSelf->Top__DOT__inst >> 0xcU)) 
            == vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
            [0U]) & vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((7U & (vlSelf->Top__DOT__inst >> 0xcU)) 
           == vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSelf->Top__DOT__inst >> 0xcU)) 
               == vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [1U]) & vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSelf->Top__DOT__inst >> 0xcU)) 
              == vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSelf->Top__DOT__inst >> 0xcU)) 
               == vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [2U]) & vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSelf->Top__DOT__inst >> 0xcU)) 
              == vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSelf->Top__DOT__inst >> 0xcU)) 
               == vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [3U]) & vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSelf->Top__DOT__inst >> 0xcU)) 
              == vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSelf->Top__DOT__inst >> 0xcU)) 
               == vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [4U]) & vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSelf->Top__DOT__inst >> 0xcU)) 
              == vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSelf->Top__DOT__inst >> 0xcU)) 
               == vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [5U]) & vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [5U]));
    vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSelf->Top__DOT__inst >> 0xcU)) 
              == vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [5U]));
    vlSelf->Top__DOT__controller_unit__DOT__take_branch 
        = ((IData)(vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           & (IData)(vlSelf->Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out));
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = vlSelf->Top__DOT__alu_result;
    vlSelf->Top__DOT__pc_unit__DOT__PCin = (((0x6fU 
                                              == (0x7fU 
                                                  & vlSelf->Top__DOT__inst)) 
                                             | ((0x67U 
                                                 == 
                                                 (0x7fU 
                                                  & vlSelf->Top__DOT__inst)) 
                                                | ((0x63U 
                                                    == 
                                                    (0x7fU 
                                                     & vlSelf->Top__DOT__inst)) 
                                                   & (IData)(vlSelf->Top__DOT__controller_unit__DOT__take_branch))))
                                             ? vlSelf->Top__DOT__alu_result
                                             : ((IData)(4U) 
                                                + vlSelf->Top__DOT__pc_out));
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
