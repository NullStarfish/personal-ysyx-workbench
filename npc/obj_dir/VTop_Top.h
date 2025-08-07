// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See VTop.h for the primary calling header

#ifndef VERILATED_VTOP_TOP_H_
#define VERILATED_VTOP_TOP_H_  // guard

#include "verilated.h"

class VTop__Syms;

class VTop_Top final : public VerilatedModule {
  public:

    // DESIGN SPECIFIC STATE
    // Anonymous structures to workaround compiler member-count bugs
    struct {
        VL_IN8(clk,0,0);
        VL_IN8(rst,0,0);
        VL_IN8(load_en,0,0);
        CData/*6:0*/ __PVT__opcode;
        CData/*4:0*/ __PVT__rd;
        CData/*4:0*/ __PVT__rs1_raw;
        CData/*4:0*/ __PVT__rs2;
        CData/*0:0*/ __PVT__Asel;
        CData/*0:0*/ __PVT__Bsel;
        CData/*0:0*/ __PVT__BrUn;
        CData/*1:0*/ __PVT__WBSel;
        CData/*2:0*/ __PVT__ImmSel;
        CData/*2:0*/ __PVT__ALUSel;
        CData/*4:0*/ __PVT__rs1_addr;
        CData/*0:0*/ __PVT__alu_br_lt;
        CData/*0:0*/ __PVT__rom_we;
        CData/*6:0*/ __PVT__controller_unit__DOT__opcode;
        CData/*2:0*/ __PVT__controller_unit__DOT__funct3;
        CData/*0:0*/ __PVT__controller_unit__DOT__take_branch;
        CData/*0:0*/ controller_unit__DOT____VdfgTmp_h7acf63f3__0;
        CData/*1:0*/ __PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out;
        CData/*0:0*/ __PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit;
        CData/*2:0*/ __PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out;
        CData/*0:0*/ __PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit;
        CData/*2:0*/ __PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out;
        CData/*0:0*/ __PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit;
        CData/*0:0*/ __PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out;
        CData/*0:0*/ __PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit;
        CData/*0:0*/ imm_gen_unit__DOT____VdfgTmp_h618f95c0__0;
        CData/*0:0*/ __PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit;
        CData/*0:0*/ __PVT__alu_unit__DOT__AdderCtrl;
        CData/*0:0*/ __PVT__alu_unit__DOT__internal_overflow;
        CData/*0:0*/ __PVT__alu_unit__DOT__internal_carry;
        CData/*0:0*/ __PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out;
        CData/*0:0*/ __PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit;
        CData/*0:0*/ __PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit;
        CData/*0:0*/ __VdfgTmp_hff5a8388__0;
        CData/*1:0*/ __VdfgTmp_h665eeada__0;
        CData/*6:0*/ __VdfgTmp_h0c27f707__0;
        CData/*0:0*/ __VdfgTmp_h144a3399__0;
        CData/*5:0*/ __VdfgTmp_h0c741b76__0;
        CData/*3:0*/ __VdfgTmp_h0ea3b6ac__0;
        CData/*7:0*/ __VdfgTmp_h0af738b7__0;
        CData/*0:0*/ __VdfgTmp_h0b17a187__0;
        SData/*11:0*/ __VdfgTmp_he0a233d6__0;
        SData/*9:0*/ __VdfgTmp_he0e8ece3__0;
        VL_IN(load_addr,31,0);
        VL_IN(load_data,31,0);
        IData/*31:0*/ pc_out;
        IData/*31:0*/ inst;
        IData/*31:0*/ __PVT__reg_rs2_data;
        IData/*31:0*/ __PVT__wb_data;
        IData/*31:0*/ __PVT__imm_out;
        IData/*31:0*/ __PVT__alu_in_a;
        IData/*31:0*/ __PVT__alu_in_b;
        IData/*31:0*/ __PVT__alu_result;
        IData/*31:0*/ __PVT__dmem_rdata;
        IData/*31:0*/ __PVT__rom_waddr;
        IData/*31:0*/ __PVT__rom_wdata;
        IData/*31:0*/ __PVT__pc_unit__DOT__PCin;
        IData/*31:0*/ __PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out;
        IData/*31:0*/ __PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out;
        IData/*31:0*/ __PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out;
        IData/*31:0*/ __PVT__alu_unit__DOT__AdderResult;
    };
    struct {
        IData/*31:0*/ __PVT__alu_unit__DOT__selAdder__DOT__t_Cin;
        IData/*31:0*/ __PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out;
        IData/*31:0*/ __VdfgTmp_hb5448552__0;
        IData/*19:0*/ __VdfgTmp_h0b35cf02__0;
        VlUnpacked<IData/*31:0*/, 32768> __PVT__imem_unit__DOT__rom0__DOT__rom_data;
        VlUnpacked<CData/*6:0*/, 3> __PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list;
        VlUnpacked<CData/*1:0*/, 3> __PVT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list;
        VlUnpacked<CData/*6:0*/, 5> __PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list;
        VlUnpacked<CData/*2:0*/, 5> __PVT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list;
        VlUnpacked<CData/*6:0*/, 2> __PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list;
        VlUnpacked<CData/*2:0*/, 2> __PVT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list;
        VlUnpacked<CData/*2:0*/, 6> __PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list;
        VlUnpacked<CData/*0:0*/, 6> __PVT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list;
        VlUnpacked<IData/*31:0*/, 32> __PVT__reg_file_unit__DOT__reg_file;
        VlUnpacked<CData/*2:0*/, 6> __PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list;
        VlUnpacked<IData/*31:0*/, 6> __PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list;
        VlUnpacked<CData/*0:0*/, 2> __PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list;
        VlUnpacked<IData/*31:0*/, 2> __PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list;
        VlUnpacked<CData/*0:0*/, 2> __PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list;
        VlUnpacked<IData/*31:0*/, 2> __PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list;
        VlUnpacked<CData/*2:0*/, 3> __PVT__alu_unit__DOT__i0__DOT__i0__DOT__key_list;
        VlUnpacked<CData/*0:0*/, 3> __PVT__alu_unit__DOT__i0__DOT__i0__DOT__data_list;
        VlUnpacked<IData/*31:0*/, 1024> __PVT__dmem_unit__DOT__mem;
        VlUnpacked<CData/*1:0*/, 3> __PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list;
        VlUnpacked<IData/*31:0*/, 3> __PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list;
    };

    // INTERNAL VARIABLES
    VTop__Syms* const vlSymsp;

    // CONSTRUCTORS
    VTop_Top(VTop__Syms* symsp, const char* v__name);
    ~VTop_Top();
    VL_UNCOPYABLE(VTop_Top);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
