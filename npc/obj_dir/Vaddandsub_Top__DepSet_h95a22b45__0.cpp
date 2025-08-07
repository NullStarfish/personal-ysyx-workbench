// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See Vaddandsub.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "Vaddandsub_Top.h"
#include "Vaddandsub__Syms.h"

void Vaddandsub___024unit____Vdpiimwrap_ebreak_TOP____024unit(IData/*31:0*/ &ebreak__Vfuncrtn);

VL_INLINE_OPT void Vaddandsub_Top___ico_sequent__TOP__Top__0(Vaddandsub_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      Vaddandsub_Top___ico_sequent__TOP__Top__0\n"); );
    // Body
    if ((0x100073U == vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) {
        Vaddandsub___024unit____Vdpiimwrap_ebreak_TOP____024unit(vlSelf->__Vtask_ebreak__0__Vfuncout);
    }
    if ((0U == (0x1fU & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                         >> 0x14U)))) {
        vlSelf->__PVT__reg_rs2_data = 0U;
        vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[1U] = 0U;
    } else {
        vlSelf->__PVT__reg_rs2_data = vlSelf->__PVT__reg_file_unit__DOT__reg_file
            [(0x1fU & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                       >> 0x14U))];
        vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[1U] 
            = vlSelf->__PVT__reg_file_unit__DOT__reg_file
            [(0x1fU & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                       >> 0x14U))];
    }
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                       == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
           == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__WBSel = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit)
                             ? (IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out)
                             : 1U);
    vlSelf->__PVT__BrUn = (IData)((0x2063U == (0x207fU 
                                               & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)));
    vlSelf->__VdfgTmp_h9f39adc8__0 = ((2U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                             >> 0x1dU)) 
                                      | (1U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                               >> 0xcU)));
    vlSelf->__PVT__Asel = ((0x17U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                           | ((0x6fU == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                              | (0x63U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data))));
    vlSelf->__PVT__rs1_addr = (((0x37U == (IData)(vlSelf->__PVT__opcode)) 
                                | (0x17U == (IData)(vlSelf->__PVT__opcode)))
                                ? 0U : (0x1fU & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                 >> 0xfU)));
    vlSelf->controller_unit__DOT____VdfgTmp_h7acf63f3__0 
        = ((0x37U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
           | ((0x17U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
              | ((0x6fU == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                 | (0x67U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)))));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = (0x1fU & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                    >> 0xfU));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = (((- (IData)((vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                        >> 0x1fU))) << 0x14U) | ((0xff000U 
                                                  & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                                                 | ((0x800U 
                                                     & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                        >> 9U)) 
                                                    | (0x7feU 
                                                       & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                          >> 0x14U)))));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[2U] 
        = (((- (IData)((vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                        >> 0x1fU))) << 0xcU) | ((0x800U 
                                                 & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                    << 4U)) 
                                                | ((0x7e0U 
                                                    & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                       >> 0x14U)) 
                                                   | (0x1eU 
                                                      & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                         >> 7U)))));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[3U] 
        = (((- (IData)((vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                        >> 0x1fU))) << 0xcU) | ((0xfe0U 
                                                 & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                    >> 0x14U)) 
                                                | (0x1fU 
                                                   & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                      >> 7U))));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[4U] 
        = (((- (IData)((vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                        >> 0x1fU))) << 0xcU) | (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                >> 0x14U));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[5U] 
        = (0xfffff000U & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data);
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                       == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
           == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [3U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [4U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->__PVT__ImmSel = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit)
                              ? (IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out)
                              : 1U);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list[1U] 
        = vlSelf->__VdfgTmp_h9f39adc8__0;
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = ((0U == (IData)(vlSelf->__PVT__rs1_addr))
            ? 0U : vlSelf->__PVT__reg_file_unit__DOT__reg_file
           [vlSelf->__PVT__rs1_addr]);
    vlSelf->__PVT__Bsel = ((0x13U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                           | ((3U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                              | ((0x23U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
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
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                       == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit 
        = ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
           == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__ALUSel = ((IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit)
                              ? (IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out)
                              : 0U);
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
        = (((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                   >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
            [0U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                  >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                      >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [1U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                     >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                      >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [2U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                     >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                      >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [3U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                     >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                      >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [4U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                     >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                      >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [5U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [5U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                     >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [5U]));
    vlSelf->__PVT__controller_unit__DOT__take_branch 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           & (IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out));
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = vlSelf->__PVT__alu_result;
    vlSelf->__PVT__pc_unit__DOT__PCin = (((0x6fU == 
                                           (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                                          | ((0x67U 
                                              == (0x7fU 
                                                  & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                                             | ((0x63U 
                                                 == 
                                                 (0x7fU 
                                                  & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                                                & (IData)(vlSelf->__PVT__controller_unit__DOT__take_branch))))
                                          ? vlSelf->__PVT__alu_result
                                          : ((IData)(4U) 
                                             + vlSelf->__PVT__pc_out));
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

VL_INLINE_OPT void Vaddandsub_Top___nba_sequent__TOP__Top__0(Vaddandsub_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      Vaddandsub_Top___nba_sequent__TOP__Top__0\n"); );
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
        if ((0x23U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data))) {
            __Vdlyvval__dmem_unit__DOT__mem__v0 = vlSelf->__PVT__reg_rs2_data;
            __Vdlyvset__dmem_unit__DOT__mem__v0 = 1U;
            __Vdlyvdim0__dmem_unit__DOT__mem__v0 = 
                (0x3ffU & (vlSelf->__PVT__alu_result 
                           >> 2U));
        }
    }
    if (vlSymsp->TOP.rst) {
        vlSelf->__PVT__dmem_rdata = 0x80000000U;
    } else if ((0x23U != (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data))) {
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

VL_INLINE_OPT void Vaddandsub_Top___nba_sequent__TOP__Top__1(Vaddandsub_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      Vaddandsub_Top___nba_sequent__TOP__Top__1\n"); );
    // Init
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
    if (vlSymsp->TOP.rst) {
        __Vdlyvset__reg_file_unit__DOT__reg_file__v0 = 1U;
    }
    if (VL_UNLIKELY((((0x33U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                      | ((0x13U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                         | ((3U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                            | (IData)(vlSelf->controller_unit__DOT____VdfgTmp_h7acf63f3__0)))) 
                     & (0U != (0x1fU & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                        >> 7U)))))) {
        VL_WRITEF("RegFile write: time=%0t AddrD=%2# DataD=%x\n",
                  64,VL_TIME_UNITED_Q(1),-12,5,(0x1fU 
                                                & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                   >> 7U)),
                  32,vlSelf->__PVT__wb_data);
        __Vdlyvval__reg_file_unit__DOT__reg_file__v32 
            = vlSelf->__PVT__wb_data;
        __Vdlyvset__reg_file_unit__DOT__reg_file__v32 = 1U;
        __Vdlyvdim0__reg_file_unit__DOT__reg_file__v32 
            = (0x1fU & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                        >> 7U));
    }
    vlSelf->__PVT__pc_out = ((IData)(vlSymsp->TOP.rst)
                              ? 0x80000000U : vlSelf->__PVT__pc_unit__DOT__PCin);
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
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = ((IData)(4U) + vlSelf->__PVT__pc_out);
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = vlSelf->__PVT__pc_out;
}

VL_INLINE_OPT void Vaddandsub_Top___nba_sequent__TOP__Top__2(Vaddandsub_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    Vaddandsub__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      Vaddandsub_Top___nba_sequent__TOP__Top__2\n"); );
    // Body
    if ((0x100073U == vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) {
        Vaddandsub___024unit____Vdpiimwrap_ebreak_TOP____024unit(vlSelf->__Vtask_ebreak__0__Vfuncout);
    }
    if ((0U == (0x1fU & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                         >> 0x14U)))) {
        vlSelf->__PVT__reg_rs2_data = 0U;
        vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[1U] = 0U;
    } else {
        vlSelf->__PVT__reg_rs2_data = vlSelf->__PVT__reg_file_unit__DOT__reg_file
            [(0x1fU & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                       >> 0x14U))];
        vlSelf->__PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list[1U] 
            = vlSelf->__PVT__reg_file_unit__DOT__reg_file
            [(0x1fU & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                       >> 0x14U))];
    }
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                       == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
           == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__WBSel = ((IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit)
                             ? (IData)(vlSelf->__PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out)
                             : 1U);
    vlSelf->__PVT__BrUn = (IData)((0x2063U == (0x207fU 
                                               & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)));
    vlSelf->__VdfgTmp_h9f39adc8__0 = ((2U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                             >> 0x1dU)) 
                                      | (1U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                               >> 0xcU)));
    vlSelf->__PVT__Asel = ((0x17U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                           | ((0x6fU == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                              | (0x63U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data))));
    vlSelf->__PVT__rs1_addr = (((0x37U == (IData)(vlSelf->__PVT__opcode)) 
                                | (0x17U == (IData)(vlSelf->__PVT__opcode)))
                                ? 0U : (0x1fU & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                 >> 0xfU)));
    vlSelf->controller_unit__DOT____VdfgTmp_h7acf63f3__0 
        = ((0x37U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
           | ((0x17U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
              | ((0x6fU == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                 | (0x67U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)))));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = (0x1fU & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                    >> 0xfU));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = (((- (IData)((vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                        >> 0x1fU))) << 0x14U) | ((0xff000U 
                                                  & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                                                 | ((0x800U 
                                                     & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                        >> 9U)) 
                                                    | (0x7feU 
                                                       & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                          >> 0x14U)))));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[2U] 
        = (((- (IData)((vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                        >> 0x1fU))) << 0xcU) | ((0x800U 
                                                 & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                    << 4U)) 
                                                | ((0x7e0U 
                                                    & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                       >> 0x14U)) 
                                                   | (0x1eU 
                                                      & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                         >> 7U)))));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[3U] 
        = (((- (IData)((vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                        >> 0x1fU))) << 0xcU) | ((0xfe0U 
                                                 & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                    >> 0x14U)) 
                                                | (0x1fU 
                                                   & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                      >> 7U))));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[4U] 
        = (((- (IData)((vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                        >> 0x1fU))) << 0xcU) | (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                                                >> 0x14U));
    vlSelf->__PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list[5U] 
        = (0xfffff000U & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data);
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                       == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
           == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [2U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [3U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
                          [4U]))) & vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->__PVT__ImmSel = ((IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit)
                              ? (IData)(vlSelf->__PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out)
                              : 1U);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list[1U] 
        = vlSelf->__VdfgTmp_h9f39adc8__0;
    vlSelf->__PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = ((0U == (IData)(vlSelf->__PVT__rs1_addr))
            ? 0U : vlSelf->__PVT__reg_file_unit__DOT__reg_file
           [vlSelf->__PVT__rs1_addr]);
    vlSelf->__PVT__Bsel = ((0x13U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                           | ((3U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                              | ((0x23U == (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
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
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out 
        = ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                       == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
                       [0U]))) & vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit 
        = ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
           == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out) 
           | ((- (IData)(((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
                          == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
                          [1U]))) & vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit) 
           | ((0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data) 
              == vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__ALUSel = ((IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit)
                              ? (IData)(vlSelf->__PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out)
                              : 0U);
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
        = (((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                   >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
            [0U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                  >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
           [0U]);
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                      >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [1U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                     >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [1U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                      >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [2U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                     >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [2U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                      >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [3U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                     >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [3U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                      >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [4U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [4U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                     >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [4U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out) 
           | (((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                      >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
               [5U]) & vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list
              [5U]));
    vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           | ((7U & (vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data 
                     >> 0xcU)) == vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list
              [5U]));
    vlSelf->__PVT__controller_unit__DOT__take_branch 
        = ((IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit) 
           & (IData)(vlSelf->__PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out));
    vlSelf->__PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[1U] 
        = vlSelf->__PVT__alu_result;
    vlSelf->__PVT__pc_unit__DOT__PCin = (((0x6fU == 
                                           (0x7fU & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                                          | ((0x67U 
                                              == (0x7fU 
                                                  & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                                             | ((0x63U 
                                                 == 
                                                 (0x7fU 
                                                  & vlSymsp->TOP__Top__imem_unit__rom0.__PVT__data)) 
                                                & (IData)(vlSelf->__PVT__controller_unit__DOT__take_branch))))
                                          ? vlSelf->__PVT__alu_result
                                          : ((IData)(4U) 
                                             + vlSelf->__PVT__pc_out));
}
