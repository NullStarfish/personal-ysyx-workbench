// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop__Syms.h"
#include "VTop___024root.h"

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

void VTop___024unit____Vdpiimwrap_ebreak_TOP____024unit(IData/*31:0*/ &ebreak__Vfuncrtn);

VL_INLINE_OPT void VTop___024root___nba_sequent__TOP__1(VTop___024root* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+    VTop___024root___nba_sequent__TOP__1\n"); );
    // Init
    CData/*0:0*/ __Vdlyvset__Top__DOT__reg_file_unit__DOT__reg_file__v0;
    __Vdlyvset__Top__DOT__reg_file_unit__DOT__reg_file__v0 = 0;
    CData/*4:0*/ __Vdlyvdim0__Top__DOT__reg_file_unit__DOT__reg_file__v32;
    __Vdlyvdim0__Top__DOT__reg_file_unit__DOT__reg_file__v32 = 0;
    IData/*31:0*/ __Vdlyvval__Top__DOT__reg_file_unit__DOT__reg_file__v32;
    __Vdlyvval__Top__DOT__reg_file_unit__DOT__reg_file__v32 = 0;
    CData/*0:0*/ __Vdlyvset__Top__DOT__reg_file_unit__DOT__reg_file__v32;
    __Vdlyvset__Top__DOT__reg_file_unit__DOT__reg_file__v32 = 0;
    // Body
    __Vdlyvset__Top__DOT__reg_file_unit__DOT__reg_file__v0 = 0U;
    __Vdlyvset__Top__DOT__reg_file_unit__DOT__reg_file__v32 = 0U;
    if (vlSelf->rst) {
        __Vdlyvset__Top__DOT__reg_file_unit__DOT__reg_file__v0 = 1U;
    }
    if (VL_UNLIKELY((((0x33U == (0x7fU & vlSelf->Top__DOT__inst)) 
                      | ((0x13U == (0x7fU & vlSelf->Top__DOT__inst)) 
                         | ((3U == (0x7fU & vlSelf->Top__DOT__inst)) 
                            | (IData)(vlSelf->Top__DOT__controller_unit__DOT____VdfgTmp_h7acf63f3__0)))) 
                     & (0U != (0x1fU & (vlSelf->Top__DOT__inst 
                                        >> 7U)))))) {
        VL_WRITEF("RegFile write: time=%0t AddrD=%2# DataD=%x\n",
                  64,VL_TIME_UNITED_Q(1000),-9,5,(0x1fU 
                                                  & (vlSelf->Top__DOT__inst 
                                                     >> 7U)),
                  32,vlSelf->Top__DOT__wb_data);
        __Vdlyvval__Top__DOT__reg_file_unit__DOT__reg_file__v32 
            = vlSelf->Top__DOT__wb_data;
        __Vdlyvset__Top__DOT__reg_file_unit__DOT__reg_file__v32 = 1U;
        __Vdlyvdim0__Top__DOT__reg_file_unit__DOT__reg_file__v32 
            = (0x1fU & (vlSelf->Top__DOT__inst >> 7U));
    }
    vlSelf->Top__DOT__pc_out = ((IData)(vlSelf->rst)
                                 ? 0U : vlSelf->Top__DOT__pc_unit__DOT__PCin);
    if (__Vdlyvset__Top__DOT__reg_file_unit__DOT__reg_file__v0) {
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[1U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[2U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[3U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[4U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[5U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[6U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[7U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[8U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[9U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0xaU] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0xbU] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0xcU] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0xdU] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0xeU] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0xfU] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x10U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x11U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x12U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x13U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x14U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x15U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x16U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x17U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x18U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x19U] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x1aU] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x1bU] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x1cU] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x1dU] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x1eU] = 0U;
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[0x1fU] = 0U;
    }
    if (__Vdlyvset__Top__DOT__reg_file_unit__DOT__reg_file__v32) {
        vlSelf->Top__DOT__reg_file_unit__DOT__reg_file[__Vdlyvdim0__Top__DOT__reg_file_unit__DOT__reg_file__v32] 
            = __Vdlyvval__Top__DOT__reg_file_unit__DOT__reg_file__v32;
    }
    vlSelf->Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list[0U] 
        = ((IData)(4U) + vlSelf->Top__DOT__pc_out);
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
}
