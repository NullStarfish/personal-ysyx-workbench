// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop_Top.h"

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
    vlSelf->__PVT__RegWEn = VL_RAND_RESET_I(1);
    vlSelf->__PVT__DMWen = VL_RAND_RESET_I(1);
    vlSelf->__PVT__Asel = VL_RAND_RESET_I(1);
    vlSelf->__PVT__Bsel = VL_RAND_RESET_I(1);
    vlSelf->__PVT__PCSel = VL_RAND_RESET_I(1);
    vlSelf->__PVT__BrUn = VL_RAND_RESET_I(1);
    vlSelf->__PVT__ForceRs1ToZero = VL_RAND_RESET_I(1);
    vlSelf->__PVT__WBSel = VL_RAND_RESET_I(2);
    vlSelf->__PVT__ImmSel = VL_RAND_RESET_I(3);
    vlSelf->__PVT__ALUSel = VL_RAND_RESET_I(4);
    vlSelf->__PVT__rs1_addr = VL_RAND_RESET_I(5);
    vlSelf->__PVT__imm_out = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_in_a = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_in_b = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_result = VL_RAND_RESET_I(32);
    vlSelf->__PVT__dmem_rdata = VL_RAND_RESET_I(32);
    vlSelf->__PVT__rom_we = VL_RAND_RESET_I(1);
    vlSelf->__PVT__rom_waddr = VL_RAND_RESET_I(32);
    vlSelf->__PVT__rom_wdata = VL_RAND_RESET_I(32);
    vlSelf->__Vcellinp__imm_gen_unit__inst_in = VL_RAND_RESET_I(25);
    vlSelf->__PVT__pc_unit__DOT__PCin = VL_RAND_RESET_I(32);
    for (int __Vi0 = 0; __Vi0 < 32768; ++__Vi0) {
        vlSelf->__PVT__imem_unit__DOT__rom0__DOT__rom_data[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->__PVT__controller_unit__DOT__opcode = VL_RAND_RESET_I(7);
    vlSelf->__PVT__controller_unit__DOT__funct3 = VL_RAND_RESET_I(3);
    vlSelf->__PVT__controller_unit__DOT__funct7_5 = VL_RAND_RESET_I(1);
    vlSelf->__PVT__alu_unit__DOT__adder_result = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_unit__DOT__shifter_result = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_unit__DOT__internal_carry = VL_RAND_RESET_I(1);
    vlSelf->__PVT__alu_unit__DOT__signed_less_than = VL_RAND_RESET_I(1);
    vlSelf->__PVT__alu_unit__DOT__adder_unit__DOT__t_Cin = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage1 = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage2 = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage3 = VL_RAND_RESET_I(32);
    vlSelf->__PVT__alu_unit__DOT__shifter_unit__DOT__stage4 = VL_RAND_RESET_I(32);
    for (int __Vi0 = 0; __Vi0 < 1024; ++__Vi0) {
        vlSelf->__PVT__dmem_unit__DOT__mem[__Vi0] = VL_RAND_RESET_I(32);
    }
    vlSelf->__VdfgTmp_hff5a8388__0 = 0;
    vlSelf->__VdfgTmp_hb5448552__0 = 0;
    vlSelf->__Vtableidx1 = 0;
    vlSelf->__Vdlyvdim0__dmem_unit__DOT__mem__v0 = 0;
    vlSelf->__Vdlyvval__dmem_unit__DOT__mem__v0 = VL_RAND_RESET_I(32);
    vlSelf->__Vdlyvset__dmem_unit__DOT__mem__v0 = 0;
}
