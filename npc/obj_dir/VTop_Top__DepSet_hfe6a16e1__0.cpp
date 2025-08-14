// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop_Top.h"
#include "VTop__Syms.h"

extern "C" void ebreak();

VL_INLINE_OPT void VTop_Top____Vdpiimwrap_controller_unit__DOT__ebreak_TOP__Top() {
    VL_DEBUG_IF(VL_DBG_MSGF("+      VTop_Top____Vdpiimwrap_controller_unit__DOT__ebreak_TOP__Top\n"); );
    // Body
    ebreak();
}

VL_INLINE_OPT void VTop_Top___ico_sequent__TOP__Top__0(VTop_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      VTop_Top___ico_sequent__TOP__Top__0\n"); );
    // Body
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
        vlSelf->__Vcellinp__imm_gen_unit__inst_in = 
            (vlSelf->__VdfgTmp_hb5448552__0 >> 7U);
        vlSelf->inst = vlSelf->__VdfgTmp_hb5448552__0;
        vlSelf->__PVT__controller_unit__DOT__opcode 
            = (0x7fU & vlSelf->__VdfgTmp_hb5448552__0);
        vlSelf->__PVT__controller_unit__DOT__funct3 
            = (7U & (vlSelf->__VdfgTmp_hb5448552__0 
                     >> 0xcU));
        vlSelf->__PVT__controller_unit__DOT__funct7_5 
            = (1U & (vlSelf->__VdfgTmp_hb5448552__0 
                     >> 0x1eU));
    } else {
        vlSelf->__Vcellinp__imm_gen_unit__inst_in = 0U;
        vlSelf->inst = 0U;
        vlSelf->__PVT__controller_unit__DOT__opcode = 0U;
        vlSelf->__PVT__controller_unit__DOT__funct3 = 0U;
        vlSelf->__PVT__controller_unit__DOT__funct7_5 = 0U;
    }
    vlSelf->__PVT__RegWEn = 0U;
    vlSelf->__PVT__DMWen = 0U;
    vlSelf->__PVT__Asel = 0U;
    vlSelf->__PVT__Bsel = 0U;
    vlSelf->__PVT__WBSel = 1U;
    vlSelf->__PVT__ImmSel = 1U;
    vlSelf->__PVT__ALUSel = 0U;
    vlSelf->__PVT__BrUn = 0U;
    vlSelf->__PVT__ForceRs1ToZero = 0U;
    if ((0x40U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
        if ((0x20U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
            if ((0x10U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                              >> 3U)))) {
                    if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                                  >> 2U)))) {
                        if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                            if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                                if (((0x73U == vlSelf->inst) 
                                     | (0x100073U == vlSelf->inst))) {
                                    VTop_Top____Vdpiimwrap_controller_unit__DOT__ebreak_TOP__Top();
                                }
                            }
                        }
                    }
                }
            } else if ((8U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((4U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                        if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                            vlSelf->__PVT__RegWEn = 1U;
                            vlSelf->__PVT__Asel = 1U;
                            vlSelf->__PVT__Bsel = 1U;
                            vlSelf->__PVT__WBSel = 2U;
                            vlSelf->__PVT__ImmSel = 4U;
                        }
                    }
                }
            } else if ((4U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                        vlSelf->__PVT__RegWEn = 1U;
                        vlSelf->__PVT__Bsel = 1U;
                        vlSelf->__PVT__WBSel = 2U;
                        vlSelf->__PVT__ImmSel = 1U;
                    }
                }
            } else if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    vlSelf->__PVT__Asel = 1U;
                    vlSelf->__PVT__Bsel = 1U;
                    vlSelf->__PVT__ImmSel = 3U;
                    vlSelf->__PVT__ALUSel = 1U;
                    if (((6U == (IData)(vlSelf->__PVT__controller_unit__DOT__funct3)) 
                         | (7U == (IData)(vlSelf->__PVT__controller_unit__DOT__funct3)))) {
                        vlSelf->__PVT__BrUn = 1U;
                    }
                }
            }
        }
    } else if ((0x20U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
        if ((0x10U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
            if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          >> 3U)))) {
                if ((4U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                        if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                            vlSelf->__PVT__RegWEn = 1U;
                            vlSelf->__PVT__Bsel = 1U;
                            vlSelf->__PVT__ImmSel = 0U;
                            vlSelf->__PVT__ForceRs1ToZero = 1U;
                        }
                    }
                } else if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                        vlSelf->__PVT__RegWEn = 1U;
                        vlSelf->__PVT__ALUSel = ((4U 
                                                  & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                  ? 
                                                 ((2U 
                                                   & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                   ? 
                                                  ((1U 
                                                    & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                    ? 2U
                                                    : 3U)
                                                   : 
                                                  ((1U 
                                                    & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                    ? 
                                                   ((IData)(vlSelf->__PVT__controller_unit__DOT__funct7_5)
                                                     ? 0xaU
                                                     : 9U)
                                                    : 4U))
                                                  : 
                                                 ((2U 
                                                   & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                   ? 
                                                  ((1U 
                                                    & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                    ? 6U
                                                    : 5U)
                                                   : 
                                                  ((1U 
                                                    & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                    ? 8U
                                                    : 
                                                   ((IData)(vlSelf->__PVT__controller_unit__DOT__funct7_5)
                                                     ? 1U
                                                     : 0U))));
                    }
                }
            }
        } else if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                             >> 3U)))) {
            if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          >> 2U)))) {
                if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                        vlSelf->__PVT__DMWen = 1U;
                        vlSelf->__PVT__Bsel = 1U;
                        vlSelf->__PVT__ImmSel = 2U;
                        vlSelf->__PVT__ALUSel = 0U;
                    }
                }
            }
        }
    } else if ((0x10U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
        if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                      >> 3U)))) {
            if ((4U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                        vlSelf->__PVT__RegWEn = 1U;
                        vlSelf->__PVT__Asel = 1U;
                        vlSelf->__PVT__Bsel = 1U;
                        vlSelf->__PVT__ImmSel = 0U;
                        vlSelf->__PVT__ForceRs1ToZero = 1U;
                    }
                }
            } else if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    vlSelf->__PVT__RegWEn = 1U;
                    vlSelf->__PVT__Bsel = 1U;
                    vlSelf->__PVT__ImmSel = 1U;
                    vlSelf->__PVT__ALUSel = ((4U & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                              ? ((2U 
                                                  & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                  ? 
                                                 ((1U 
                                                   & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                   ? 2U
                                                   : 3U)
                                                  : 
                                                 ((1U 
                                                   & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                   ? 
                                                  ((IData)(vlSelf->__PVT__controller_unit__DOT__funct7_5)
                                                    ? 0xaU
                                                    : 9U)
                                                   : 4U))
                                              : ((2U 
                                                  & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                  ? 
                                                 ((1U 
                                                   & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                   ? 6U
                                                   : 5U)
                                                  : 
                                                 ((1U 
                                                   & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                   ? 8U
                                                   : 0U)));
                }
            }
        }
    } else if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                         >> 3U)))) {
        if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                      >> 2U)))) {
            if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    vlSelf->__PVT__RegWEn = 1U;
                    vlSelf->__PVT__Bsel = 1U;
                    vlSelf->__PVT__WBSel = 0U;
                    vlSelf->__PVT__ImmSel = 1U;
                    vlSelf->__PVT__ALUSel = 0U;
                }
            }
        }
    }
    vlSelf->__PVT__rs1_addr = ((IData)(vlSelf->__PVT__ForceRs1ToZero)
                                ? 0U : ((IData)(vlSelf->__VdfgTmp_hff5a8388__0)
                                         ? (0x1fU & 
                                            (vlSelf->__VdfgTmp_hb5448552__0 
                                             >> 0xfU))
                                         : 0U));
    vlSelf->__PVT__imm_out = ((4U & (IData)(vlSelf->__PVT__ImmSel))
                               ? ((2U & (IData)(vlSelf->__PVT__ImmSel))
                                   ? 0U : ((1U & (IData)(vlSelf->__PVT__ImmSel))
                                            ? (0x1fU 
                                               & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                  >> 8U))
                                            : (((- (IData)(
                                                           (1U 
                                                            & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                               >> 0x18U)))) 
                                                << 0x14U) 
                                               | ((0xff000U 
                                                   & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                      << 7U)) 
                                                  | ((0x800U 
                                                      & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                         >> 2U)) 
                                                     | (0x7feU 
                                                        & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                           >> 0xdU)))))))
                               : ((2U & (IData)(vlSelf->__PVT__ImmSel))
                                   ? ((1U & (IData)(vlSelf->__PVT__ImmSel))
                                       ? (((- (IData)(
                                                      (1U 
                                                       & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                          >> 0x18U)))) 
                                           << 0xcU) 
                                          | ((0x800U 
                                              & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                 << 0xbU)) 
                                             | ((0x7e0U 
                                                 & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                    >> 0xdU)) 
                                                | (0x1eU 
                                                   & vlSelf->__Vcellinp__imm_gen_unit__inst_in))))
                                       : (((- (IData)(
                                                      (1U 
                                                       & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                          >> 0x18U)))) 
                                           << 0xcU) 
                                          | ((0xfe0U 
                                              & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                 >> 0xdU)) 
                                             | (0x1fU 
                                                & vlSelf->__Vcellinp__imm_gen_unit__inst_in))))
                                   : ((1U & (IData)(vlSelf->__PVT__ImmSel))
                                       ? (((- (IData)(
                                                      (1U 
                                                       & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                          >> 0x18U)))) 
                                           << 0xcU) 
                                          | (0xfffU 
                                             & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                >> 0xdU)))
                                       : (0xfffff000U 
                                          & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                             << 7U)))));
    vlSelf->__PVT__alu_in_a = ((IData)(vlSelf->__PVT__Asel)
                                ? vlSelf->pc_out : 
                               ((0U == (IData)(vlSelf->__PVT__rs1_addr))
                                 ? 0U : vlSymsp->TOP__Top__reg_file_unit.reg_file
                                [vlSelf->__PVT__rs1_addr]));
}

extern const VlUnpacked<CData/*0:0*/, 128> VTop__ConstPool__TABLE_h2883d3f2_0;

VL_INLINE_OPT void VTop_Top___ico_sequent__TOP__Top__1(VTop_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      VTop_Top___ico_sequent__TOP__Top__1\n"); );
    // Body
    vlSelf->__PVT__alu_in_b = ((IData)(vlSelf->__PVT__Bsel)
                                ? vlSelf->__PVT__imm_out
                                : vlSymsp->TOP__Top__reg_file_unit.__PVT__DataB);
    vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1 
        = ((0x10U & vlSelf->__PVT__alu_in_b) ? ((0U 
                                                 == 
                                                 (3U 
                                                  & (IData)(vlSelf->__PVT__ALUSel)))
                                                 ? 
                                                (vlSelf->__PVT__alu_in_a 
                                                 << 0x10U)
                                                 : 
                                                ((2U 
                                                  == 
                                                  (3U 
                                                   & (IData)(vlSelf->__PVT__ALUSel)))
                                                  ? 
                                                 (((- (IData)(
                                                              (vlSelf->__PVT__alu_in_a 
                                                               >> 0x1fU))) 
                                                   << 0x10U) 
                                                  | (vlSelf->__PVT__alu_in_a 
                                                     >> 0x10U))
                                                  : 
                                                 (vlSelf->__PVT__alu_in_a 
                                                  >> 0x10U)))
            : vlSelf->__PVT__alu_in_a);
    vlSelf->__PVT__alu_unit__DOT__adder_unit__DOT__t_Cin 
        = (((- (IData)((1U == (IData)(vlSelf->__PVT__ALUSel)))) 
            ^ vlSelf->__PVT__alu_in_b) + (1U == (IData)(vlSelf->__PVT__ALUSel)));
    vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2 
        = ((8U & vlSelf->__PVT__alu_in_b) ? ((0U == 
                                              (3U & (IData)(vlSelf->__PVT__ALUSel)))
                                              ? (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1 
                                                 << 8U)
                                              : ((2U 
                                                  == 
                                                  (3U 
                                                   & (IData)(vlSelf->__PVT__ALUSel)))
                                                  ? 
                                                 (((- (IData)(
                                                              (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1 
                                                               >> 0x1fU))) 
                                                   << 0x18U) 
                                                  | (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1 
                                                     >> 8U))
                                                  : 
                                                 (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1 
                                                  >> 8U)))
            : vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1);
    vlSelf->__PVT__alu_unit__DOT__internal_carry = 
        (1U & (IData)((1ULL & (((QData)((IData)(vlSelf->__PVT__alu_in_a)) 
                                + (QData)((IData)(vlSelf->__PVT__alu_unit__DOT__adder_unit__DOT__t_Cin))) 
                               >> 0x20U))));
    vlSelf->__PVT__alu_unit__DOT__adder_result = (vlSelf->__PVT__alu_in_a 
                                                  + vlSelf->__PVT__alu_unit__DOT__adder_unit__DOT__t_Cin);
    vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3 
        = ((4U & vlSelf->__PVT__alu_in_b) ? ((0U == 
                                              (3U & (IData)(vlSelf->__PVT__ALUSel)))
                                              ? (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2 
                                                 << 4U)
                                              : ((2U 
                                                  == 
                                                  (3U 
                                                   & (IData)(vlSelf->__PVT__ALUSel)))
                                                  ? 
                                                 (((- (IData)(
                                                              (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2 
                                                               >> 0x1fU))) 
                                                   << 0x1cU) 
                                                  | (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2 
                                                     >> 4U))
                                                  : 
                                                 (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2 
                                                  >> 4U)))
            : vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2);
    vlSelf->__PVT__alu_unit__DOT__signed_less_than 
        = (1U & ((((vlSelf->__PVT__alu_in_a >> 0x1fU) 
                   == (vlSelf->__PVT__alu_in_b >> 0x1fU)) 
                  & ((vlSelf->__PVT__alu_unit__DOT__adder_result 
                      >> 0x1fU) != (vlSelf->__PVT__alu_in_a 
                                    >> 0x1fU))) ^ (vlSelf->__PVT__alu_unit__DOT__adder_result 
                                                   >> 0x1fU)));
    vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4 
        = ((2U & vlSelf->__PVT__alu_in_b) ? ((0U == 
                                              (3U & (IData)(vlSelf->__PVT__ALUSel)))
                                              ? (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3 
                                                 << 2U)
                                              : ((2U 
                                                  == 
                                                  (3U 
                                                   & (IData)(vlSelf->__PVT__ALUSel)))
                                                  ? 
                                                 (((- (IData)(
                                                              (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3 
                                                               >> 0x1fU))) 
                                                   << 0x1eU) 
                                                  | (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3 
                                                     >> 2U))
                                                  : 
                                                 (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3 
                                                  >> 2U)))
            : vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3);
    vlSelf->__Vtableidx1 = ((0x40U & ((~ (IData)((0U 
                                                  != vlSelf->__PVT__alu_unit__DOT__adder_result))) 
                                      << 6U)) | ((0x20U 
                                                  & (((IData)(vlSelf->__PVT__BrUn)
                                                       ? 
                                                      (~ (IData)(vlSelf->__PVT__alu_unit__DOT__internal_carry))
                                                       : (IData)(vlSelf->__PVT__alu_unit__DOT__signed_less_than)) 
                                                     << 5U)) 
                                                 | (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
                                                     << 2U) 
                                                    | (((0x63U 
                                                         == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                                                        << 1U) 
                                                       | ((0x6fU 
                                                           == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                                                          | (0x67U 
                                                             == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)))))));
    vlSelf->__PVT__PCSel = VTop__ConstPool__TABLE_h2883d3f2_0
        [vlSelf->__Vtableidx1];
    vlSelf->__PVT__alu_unit__DOT__shifter_result = 
        ((1U & vlSelf->__PVT__alu_in_b) ? ((0U == (3U 
                                                   & (IData)(vlSelf->__PVT__ALUSel)))
                                            ? (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4 
                                               << 1U)
                                            : ((2U 
                                                == 
                                                (3U 
                                                 & (IData)(vlSelf->__PVT__ALUSel)))
                                                ? (
                                                   (0x80000000U 
                                                    & vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4) 
                                                   | (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4 
                                                      >> 1U))
                                                : (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4 
                                                   >> 1U)))
          : vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4);
    vlSelf->__PVT__alu_result = ((8U & (IData)(vlSelf->__PVT__ALUSel))
                                  ? ((4U & (IData)(vlSelf->__PVT__ALUSel))
                                      ? 0xdeadbeefU
                                      : ((2U & (IData)(vlSelf->__PVT__ALUSel))
                                          ? ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                              ? 0xdeadbeefU
                                              : vlSelf->__PVT__alu_unit__DOT__shifter_result)
                                          : vlSelf->__PVT__alu_unit__DOT__shifter_result))
                                  : ((4U & (IData)(vlSelf->__PVT__ALUSel))
                                      ? ((2U & (IData)(vlSelf->__PVT__ALUSel))
                                          ? ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                              ? 0xdeadbeefU
                                              : (1U 
                                                 & (~ (IData)(vlSelf->__PVT__alu_unit__DOT__internal_carry))))
                                          : ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                              ? (IData)(vlSelf->__PVT__alu_unit__DOT__signed_less_than)
                                              : (vlSelf->__PVT__alu_in_a 
                                                 ^ vlSelf->__PVT__alu_in_b)))
                                      : ((2U & (IData)(vlSelf->__PVT__ALUSel))
                                          ? ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                              ? (vlSelf->__PVT__alu_in_a 
                                                 | vlSelf->__PVT__alu_in_b)
                                              : (vlSelf->__PVT__alu_in_a 
                                                 & vlSelf->__PVT__alu_in_b))
                                          : vlSelf->__PVT__alu_unit__DOT__adder_result)));
    vlSelf->__PVT__pc_unit__DOT__PCin = ((IData)(vlSelf->__PVT__PCSel)
                                          ? vlSelf->__PVT__alu_result
                                          : ((IData)(4U) 
                                             + vlSelf->pc_out));
}

VL_INLINE_OPT void VTop_Top___nba_sequent__TOP__Top__0(VTop_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      VTop_Top___nba_sequent__TOP__Top__0\n"); );
    // Body
    vlSelf->__Vdlyvset__dmem_unit__DOT__mem__v0 = 0U;
    if ((1U & (~ (IData)(vlSymsp->TOP.rst)))) {
        if (vlSelf->__PVT__DMWen) {
            vlSelf->__Vdlyvval__dmem_unit__DOT__mem__v0 
                = vlSymsp->TOP__Top__reg_file_unit.__PVT__DataB;
            vlSelf->__Vdlyvset__dmem_unit__DOT__mem__v0 = 1U;
            vlSelf->__Vdlyvdim0__dmem_unit__DOT__mem__v0 
                = (0x3ffU & (vlSelf->__PVT__alu_result 
                             >> 2U));
        }
    }
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
    // Body
    __Vdlyvset__imem_unit__DOT__rom0__DOT__rom_data__v0 = 0U;
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
    if (__Vdlyvset__imem_unit__DOT__rom0__DOT__rom_data__v0) {
        vlSelf->__PVT__imem_unit__DOT__rom0__DOT__rom_data[__Vdlyvdim0__imem_unit__DOT__rom0__DOT__rom_data__v0] 
            = __Vdlyvval__imem_unit__DOT__rom0__DOT__rom_data__v0;
    }
    vlSelf->__PVT__rom_we = ((~ (IData)(vlSymsp->TOP.rst)) 
                             & (IData)(vlSymsp->TOP.load_en));
    if (vlSymsp->TOP.rst) {
        vlSelf->__PVT__rom_waddr = 0U;
        vlSelf->__PVT__rom_wdata = 0U;
    } else if (vlSymsp->TOP.load_en) {
        vlSelf->__PVT__rom_waddr = vlSymsp->TOP.load_addr;
        vlSelf->__PVT__rom_wdata = vlSymsp->TOP.load_data;
    }
}

VL_INLINE_OPT void VTop_Top___nba_sequent__TOP__Top__2(VTop_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      VTop_Top___nba_sequent__TOP__Top__2\n"); );
    // Body
    vlSelf->pc_out = ((IData)(vlSymsp->TOP.rst) ? 0x80000000U
                       : vlSelf->__PVT__pc_unit__DOT__PCin);
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
        vlSelf->__Vcellinp__imm_gen_unit__inst_in = 
            (vlSelf->__VdfgTmp_hb5448552__0 >> 7U);
        vlSelf->inst = vlSelf->__VdfgTmp_hb5448552__0;
        vlSelf->__PVT__controller_unit__DOT__opcode 
            = (0x7fU & vlSelf->__VdfgTmp_hb5448552__0);
        vlSelf->__PVT__controller_unit__DOT__funct3 
            = (7U & (vlSelf->__VdfgTmp_hb5448552__0 
                     >> 0xcU));
        vlSelf->__PVT__controller_unit__DOT__funct7_5 
            = (1U & (vlSelf->__VdfgTmp_hb5448552__0 
                     >> 0x1eU));
    } else {
        vlSelf->__Vcellinp__imm_gen_unit__inst_in = 0U;
        vlSelf->inst = 0U;
        vlSelf->__PVT__controller_unit__DOT__opcode = 0U;
        vlSelf->__PVT__controller_unit__DOT__funct3 = 0U;
        vlSelf->__PVT__controller_unit__DOT__funct7_5 = 0U;
    }
}

VL_INLINE_OPT void VTop_Top___nba_sequent__TOP__Top__3(VTop_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      VTop_Top___nba_sequent__TOP__Top__3\n"); );
    // Body
    if (vlSymsp->TOP.rst) {
        vlSelf->__PVT__dmem_rdata = 0x80000000U;
    } else if ((1U & (~ (IData)(vlSelf->__PVT__DMWen)))) {
        vlSelf->__PVT__dmem_rdata = vlSelf->__PVT__dmem_unit__DOT__mem
            [(0x3ffU & (vlSelf->__PVT__alu_result >> 2U))];
    }
    if (vlSelf->__Vdlyvset__dmem_unit__DOT__mem__v0) {
        vlSelf->__PVT__dmem_unit__DOT__mem[vlSelf->__Vdlyvdim0__dmem_unit__DOT__mem__v0] 
            = vlSelf->__Vdlyvval__dmem_unit__DOT__mem__v0;
    }
}

VL_INLINE_OPT void VTop_Top___nba_sequent__TOP__Top__4(VTop_Top* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+      VTop_Top___nba_sequent__TOP__Top__4\n"); );
    // Body
    vlSelf->__PVT__RegWEn = 0U;
    vlSelf->__PVT__DMWen = 0U;
    vlSelf->__PVT__Asel = 0U;
    vlSelf->__PVT__Bsel = 0U;
    vlSelf->__PVT__WBSel = 1U;
    vlSelf->__PVT__ImmSel = 1U;
    vlSelf->__PVT__ALUSel = 0U;
    vlSelf->__PVT__BrUn = 0U;
    vlSelf->__PVT__ForceRs1ToZero = 0U;
    if ((0x40U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
        if ((0x20U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
            if ((0x10U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                              >> 3U)))) {
                    if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                                  >> 2U)))) {
                        if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                            if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                                if (((0x73U == vlSelf->inst) 
                                     | (0x100073U == vlSelf->inst))) {
                                    VTop_Top____Vdpiimwrap_controller_unit__DOT__ebreak_TOP__Top();
                                }
                            }
                        }
                    }
                }
            } else if ((8U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((4U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                        if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                            vlSelf->__PVT__RegWEn = 1U;
                            vlSelf->__PVT__Asel = 1U;
                            vlSelf->__PVT__Bsel = 1U;
                            vlSelf->__PVT__WBSel = 2U;
                            vlSelf->__PVT__ImmSel = 4U;
                        }
                    }
                }
            } else if ((4U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                        vlSelf->__PVT__RegWEn = 1U;
                        vlSelf->__PVT__Bsel = 1U;
                        vlSelf->__PVT__WBSel = 2U;
                        vlSelf->__PVT__ImmSel = 1U;
                    }
                }
            } else if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    vlSelf->__PVT__Asel = 1U;
                    vlSelf->__PVT__Bsel = 1U;
                    vlSelf->__PVT__ImmSel = 3U;
                    vlSelf->__PVT__ALUSel = 1U;
                    if (((6U == (IData)(vlSelf->__PVT__controller_unit__DOT__funct3)) 
                         | (7U == (IData)(vlSelf->__PVT__controller_unit__DOT__funct3)))) {
                        vlSelf->__PVT__BrUn = 1U;
                    }
                }
            }
        }
    } else if ((0x20U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
        if ((0x10U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
            if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          >> 3U)))) {
                if ((4U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                        if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                            vlSelf->__PVT__RegWEn = 1U;
                            vlSelf->__PVT__Bsel = 1U;
                            vlSelf->__PVT__ImmSel = 0U;
                            vlSelf->__PVT__ForceRs1ToZero = 1U;
                        }
                    }
                } else if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                        vlSelf->__PVT__RegWEn = 1U;
                        vlSelf->__PVT__ALUSel = ((4U 
                                                  & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                  ? 
                                                 ((2U 
                                                   & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                   ? 
                                                  ((1U 
                                                    & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                    ? 2U
                                                    : 3U)
                                                   : 
                                                  ((1U 
                                                    & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                    ? 
                                                   ((IData)(vlSelf->__PVT__controller_unit__DOT__funct7_5)
                                                     ? 0xaU
                                                     : 9U)
                                                    : 4U))
                                                  : 
                                                 ((2U 
                                                   & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                   ? 
                                                  ((1U 
                                                    & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                    ? 6U
                                                    : 5U)
                                                   : 
                                                  ((1U 
                                                    & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                    ? 8U
                                                    : 
                                                   ((IData)(vlSelf->__PVT__controller_unit__DOT__funct7_5)
                                                     ? 1U
                                                     : 0U))));
                    }
                }
            }
        } else if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                             >> 3U)))) {
            if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                          >> 2U)))) {
                if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                        vlSelf->__PVT__DMWen = 1U;
                        vlSelf->__PVT__Bsel = 1U;
                        vlSelf->__PVT__ImmSel = 2U;
                        vlSelf->__PVT__ALUSel = 0U;
                    }
                }
            }
        }
    } else if ((0x10U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
        if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                      >> 3U)))) {
            if ((4U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                        vlSelf->__PVT__RegWEn = 1U;
                        vlSelf->__PVT__Asel = 1U;
                        vlSelf->__PVT__Bsel = 1U;
                        vlSelf->__PVT__ImmSel = 0U;
                        vlSelf->__PVT__ForceRs1ToZero = 1U;
                    }
                }
            } else if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    vlSelf->__PVT__RegWEn = 1U;
                    vlSelf->__PVT__Bsel = 1U;
                    vlSelf->__PVT__ImmSel = 1U;
                    vlSelf->__PVT__ALUSel = ((4U & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                              ? ((2U 
                                                  & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                  ? 
                                                 ((1U 
                                                   & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                   ? 2U
                                                   : 3U)
                                                  : 
                                                 ((1U 
                                                   & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                   ? 
                                                  ((IData)(vlSelf->__PVT__controller_unit__DOT__funct7_5)
                                                    ? 0xaU
                                                    : 9U)
                                                   : 4U))
                                              : ((2U 
                                                  & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                  ? 
                                                 ((1U 
                                                   & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                   ? 6U
                                                   : 5U)
                                                  : 
                                                 ((1U 
                                                   & (IData)(vlSelf->__PVT__controller_unit__DOT__funct3))
                                                   ? 8U
                                                   : 0U)));
                }
            }
        }
    } else if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                         >> 3U)))) {
        if ((1U & (~ ((IData)(vlSelf->__PVT__controller_unit__DOT__opcode) 
                      >> 2U)))) {
            if ((2U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                if ((1U & (IData)(vlSelf->__PVT__controller_unit__DOT__opcode))) {
                    vlSelf->__PVT__RegWEn = 1U;
                    vlSelf->__PVT__Bsel = 1U;
                    vlSelf->__PVT__WBSel = 0U;
                    vlSelf->__PVT__ImmSel = 1U;
                    vlSelf->__PVT__ALUSel = 0U;
                }
            }
        }
    }
    vlSelf->__PVT__rs1_addr = ((IData)(vlSelf->__PVT__ForceRs1ToZero)
                                ? 0U : ((IData)(vlSelf->__VdfgTmp_hff5a8388__0)
                                         ? (0x1fU & 
                                            (vlSelf->__VdfgTmp_hb5448552__0 
                                             >> 0xfU))
                                         : 0U));
    vlSelf->__PVT__imm_out = ((4U & (IData)(vlSelf->__PVT__ImmSel))
                               ? ((2U & (IData)(vlSelf->__PVT__ImmSel))
                                   ? 0U : ((1U & (IData)(vlSelf->__PVT__ImmSel))
                                            ? (0x1fU 
                                               & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                  >> 8U))
                                            : (((- (IData)(
                                                           (1U 
                                                            & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                               >> 0x18U)))) 
                                                << 0x14U) 
                                               | ((0xff000U 
                                                   & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                      << 7U)) 
                                                  | ((0x800U 
                                                      & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                         >> 2U)) 
                                                     | (0x7feU 
                                                        & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                           >> 0xdU)))))))
                               : ((2U & (IData)(vlSelf->__PVT__ImmSel))
                                   ? ((1U & (IData)(vlSelf->__PVT__ImmSel))
                                       ? (((- (IData)(
                                                      (1U 
                                                       & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                          >> 0x18U)))) 
                                           << 0xcU) 
                                          | ((0x800U 
                                              & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                 << 0xbU)) 
                                             | ((0x7e0U 
                                                 & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                    >> 0xdU)) 
                                                | (0x1eU 
                                                   & vlSelf->__Vcellinp__imm_gen_unit__inst_in))))
                                       : (((- (IData)(
                                                      (1U 
                                                       & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                          >> 0x18U)))) 
                                           << 0xcU) 
                                          | ((0xfe0U 
                                              & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                 >> 0xdU)) 
                                             | (0x1fU 
                                                & vlSelf->__Vcellinp__imm_gen_unit__inst_in))))
                                   : ((1U & (IData)(vlSelf->__PVT__ImmSel))
                                       ? (((- (IData)(
                                                      (1U 
                                                       & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                          >> 0x18U)))) 
                                           << 0xcU) 
                                          | (0xfffU 
                                             & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                                >> 0xdU)))
                                       : (0xfffff000U 
                                          & (vlSelf->__Vcellinp__imm_gen_unit__inst_in 
                                             << 7U)))));
    vlSelf->__PVT__alu_in_a = ((IData)(vlSelf->__PVT__Asel)
                                ? vlSelf->pc_out : 
                               ((0U == (IData)(vlSelf->__PVT__rs1_addr))
                                 ? 0U : vlSymsp->TOP__Top__reg_file_unit.reg_file
                                [vlSelf->__PVT__rs1_addr]));
    vlSelf->__PVT__alu_in_b = ((IData)(vlSelf->__PVT__Bsel)
                                ? vlSelf->__PVT__imm_out
                                : vlSymsp->TOP__Top__reg_file_unit.__PVT__DataB);
    vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1 
        = ((0x10U & vlSelf->__PVT__alu_in_b) ? ((0U 
                                                 == 
                                                 (3U 
                                                  & (IData)(vlSelf->__PVT__ALUSel)))
                                                 ? 
                                                (vlSelf->__PVT__alu_in_a 
                                                 << 0x10U)
                                                 : 
                                                ((2U 
                                                  == 
                                                  (3U 
                                                   & (IData)(vlSelf->__PVT__ALUSel)))
                                                  ? 
                                                 (((- (IData)(
                                                              (vlSelf->__PVT__alu_in_a 
                                                               >> 0x1fU))) 
                                                   << 0x10U) 
                                                  | (vlSelf->__PVT__alu_in_a 
                                                     >> 0x10U))
                                                  : 
                                                 (vlSelf->__PVT__alu_in_a 
                                                  >> 0x10U)))
            : vlSelf->__PVT__alu_in_a);
    vlSelf->__PVT__alu_unit__DOT__adder_unit__DOT__t_Cin 
        = (((- (IData)((1U == (IData)(vlSelf->__PVT__ALUSel)))) 
            ^ vlSelf->__PVT__alu_in_b) + (1U == (IData)(vlSelf->__PVT__ALUSel)));
    vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2 
        = ((8U & vlSelf->__PVT__alu_in_b) ? ((0U == 
                                              (3U & (IData)(vlSelf->__PVT__ALUSel)))
                                              ? (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1 
                                                 << 8U)
                                              : ((2U 
                                                  == 
                                                  (3U 
                                                   & (IData)(vlSelf->__PVT__ALUSel)))
                                                  ? 
                                                 (((- (IData)(
                                                              (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1 
                                                               >> 0x1fU))) 
                                                   << 0x18U) 
                                                  | (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1 
                                                     >> 8U))
                                                  : 
                                                 (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1 
                                                  >> 8U)))
            : vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1);
    vlSelf->__PVT__alu_unit__DOT__internal_carry = 
        (1U & (IData)((1ULL & (((QData)((IData)(vlSelf->__PVT__alu_in_a)) 
                                + (QData)((IData)(vlSelf->__PVT__alu_unit__DOT__adder_unit__DOT__t_Cin))) 
                               >> 0x20U))));
    vlSelf->__PVT__alu_unit__DOT__adder_result = (vlSelf->__PVT__alu_in_a 
                                                  + vlSelf->__PVT__alu_unit__DOT__adder_unit__DOT__t_Cin);
    vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3 
        = ((4U & vlSelf->__PVT__alu_in_b) ? ((0U == 
                                              (3U & (IData)(vlSelf->__PVT__ALUSel)))
                                              ? (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2 
                                                 << 4U)
                                              : ((2U 
                                                  == 
                                                  (3U 
                                                   & (IData)(vlSelf->__PVT__ALUSel)))
                                                  ? 
                                                 (((- (IData)(
                                                              (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2 
                                                               >> 0x1fU))) 
                                                   << 0x1cU) 
                                                  | (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2 
                                                     >> 4U))
                                                  : 
                                                 (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2 
                                                  >> 4U)))
            : vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2);
    vlSelf->__PVT__alu_unit__DOT__signed_less_than 
        = (1U & ((((vlSelf->__PVT__alu_in_a >> 0x1fU) 
                   == (vlSelf->__PVT__alu_in_b >> 0x1fU)) 
                  & ((vlSelf->__PVT__alu_unit__DOT__adder_result 
                      >> 0x1fU) != (vlSelf->__PVT__alu_in_a 
                                    >> 0x1fU))) ^ (vlSelf->__PVT__alu_unit__DOT__adder_result 
                                                   >> 0x1fU)));
    vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4 
        = ((2U & vlSelf->__PVT__alu_in_b) ? ((0U == 
                                              (3U & (IData)(vlSelf->__PVT__ALUSel)))
                                              ? (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3 
                                                 << 2U)
                                              : ((2U 
                                                  == 
                                                  (3U 
                                                   & (IData)(vlSelf->__PVT__ALUSel)))
                                                  ? 
                                                 (((- (IData)(
                                                              (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3 
                                                               >> 0x1fU))) 
                                                   << 0x1eU) 
                                                  | (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3 
                                                     >> 2U))
                                                  : 
                                                 (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3 
                                                  >> 2U)))
            : vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3);
    vlSelf->__Vtableidx1 = ((0x40U & ((~ (IData)((0U 
                                                  != vlSelf->__PVT__alu_unit__DOT__adder_result))) 
                                      << 6U)) | ((0x20U 
                                                  & (((IData)(vlSelf->__PVT__BrUn)
                                                       ? 
                                                      (~ (IData)(vlSelf->__PVT__alu_unit__DOT__internal_carry))
                                                       : (IData)(vlSelf->__PVT__alu_unit__DOT__signed_less_than)) 
                                                     << 5U)) 
                                                 | (((IData)(vlSelf->__PVT__controller_unit__DOT__funct3) 
                                                     << 2U) 
                                                    | (((0x63U 
                                                         == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                                                        << 1U) 
                                                       | ((0x6fU 
                                                           == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)) 
                                                          | (0x67U 
                                                             == (IData)(vlSelf->__PVT__controller_unit__DOT__opcode)))))));
    vlSelf->__PVT__PCSel = VTop__ConstPool__TABLE_h2883d3f2_0
        [vlSelf->__Vtableidx1];
    vlSelf->__PVT__alu_unit__DOT__shifter_result = 
        ((1U & vlSelf->__PVT__alu_in_b) ? ((0U == (3U 
                                                   & (IData)(vlSelf->__PVT__ALUSel)))
                                            ? (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4 
                                               << 1U)
                                            : ((2U 
                                                == 
                                                (3U 
                                                 & (IData)(vlSelf->__PVT__ALUSel)))
                                                ? (
                                                   (0x80000000U 
                                                    & vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4) 
                                                   | (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4 
                                                      >> 1U))
                                                : (vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4 
                                                   >> 1U)))
          : vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4);
    vlSelf->__PVT__alu_result = ((8U & (IData)(vlSelf->__PVT__ALUSel))
                                  ? ((4U & (IData)(vlSelf->__PVT__ALUSel))
                                      ? 0xdeadbeefU
                                      : ((2U & (IData)(vlSelf->__PVT__ALUSel))
                                          ? ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                              ? 0xdeadbeefU
                                              : vlSelf->__PVT__alu_unit__DOT__shifter_result)
                                          : vlSelf->__PVT__alu_unit__DOT__shifter_result))
                                  : ((4U & (IData)(vlSelf->__PVT__ALUSel))
                                      ? ((2U & (IData)(vlSelf->__PVT__ALUSel))
                                          ? ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                              ? 0xdeadbeefU
                                              : (1U 
                                                 & (~ (IData)(vlSelf->__PVT__alu_unit__DOT__internal_carry))))
                                          : ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                              ? (IData)(vlSelf->__PVT__alu_unit__DOT__signed_less_than)
                                              : (vlSelf->__PVT__alu_in_a 
                                                 ^ vlSelf->__PVT__alu_in_b)))
                                      : ((2U & (IData)(vlSelf->__PVT__ALUSel))
                                          ? ((1U & (IData)(vlSelf->__PVT__ALUSel))
                                              ? (vlSelf->__PVT__alu_in_a 
                                                 | vlSelf->__PVT__alu_in_b)
                                              : (vlSelf->__PVT__alu_in_a 
                                                 & vlSelf->__PVT__alu_in_b))
                                          : vlSelf->__PVT__alu_unit__DOT__adder_result)));
    vlSelf->__PVT__pc_unit__DOT__PCin = ((IData)(vlSelf->__PVT__PCSel)
                                          ? vlSelf->__PVT__alu_result
                                          : ((IData)(4U) 
                                             + vlSelf->pc_out));
}
