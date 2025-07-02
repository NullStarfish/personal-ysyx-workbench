// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See VTop.h for the primary calling header

#ifndef VERILATED_VTOP___024ROOT_H_
#define VERILATED_VTOP___024ROOT_H_  // guard

#include "verilated.h"

class VTop__Syms;
class VTop___024unit;


class VTop___024root final : public VerilatedModule {
  public:
    // CELLS
    VTop___024unit* __PVT____024unit;

    // DESIGN SPECIFIC STATE
    // Anonymous structures to workaround compiler member-count bugs
    struct {
        VL_IN8(clk,0,0);
        VL_IN8(rst,0,0);
        CData/*6:0*/ Top__DOT__opcode;
        CData/*0:0*/ Top__DOT__Asel;
        CData/*0:0*/ Top__DOT__Bsel;
        CData/*0:0*/ Top__DOT__BrUn;
        CData/*1:0*/ Top__DOT__WBSel;
        CData/*2:0*/ Top__DOT__ImmSel;
        CData/*2:0*/ Top__DOT__ALUSel;
        CData/*4:0*/ Top__DOT__rs1_addr;
        CData/*0:0*/ Top__DOT__alu_br_lt;
        CData/*0:0*/ Top__DOT__controller_unit__DOT__take_branch;
        CData/*0:0*/ Top__DOT__controller_unit__DOT____VdfgTmp_h7acf63f3__0;
        CData/*1:0*/ Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__lut_out;
        CData/*0:0*/ Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__hit;
        CData/*2:0*/ Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__lut_out;
        CData/*0:0*/ Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__hit;
        CData/*2:0*/ Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__lut_out;
        CData/*0:0*/ Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__hit;
        CData/*0:0*/ Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__lut_out;
        CData/*0:0*/ Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__hit;
        CData/*0:0*/ Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit;
        CData/*0:0*/ Top__DOT__alu_unit__DOT__AdderCtrl;
        CData/*0:0*/ Top__DOT__alu_unit__DOT__internal_overflow;
        CData/*0:0*/ Top__DOT__alu_unit__DOT__internal_carry;
        CData/*0:0*/ Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out;
        CData/*0:0*/ Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__hit;
        CData/*0:0*/ Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit;
        CData/*1:0*/ __VdfgTmp_h2ee91c3a__0;
        CData/*0:0*/ __Vtrigrprev__TOP__clk;
        CData/*0:0*/ __Vtrigrprev__TOP__rst;
        CData/*0:0*/ __VactContinue;
        IData/*31:0*/ Top__DOT__pc_out;
        IData/*31:0*/ Top__DOT__inst;
        IData/*31:0*/ Top__DOT__reg_rs2_data;
        IData/*31:0*/ Top__DOT__wb_data;
        IData/*31:0*/ Top__DOT__imm_out;
        IData/*31:0*/ Top__DOT__alu_in_a;
        IData/*31:0*/ Top__DOT__alu_in_b;
        IData/*31:0*/ Top__DOT__alu_result;
        IData/*31:0*/ Top__DOT__dmem_rdata;
        IData/*31:0*/ Top__DOT__pc_unit__DOT__PCin;
        IData/*31:0*/ Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out;
        IData/*31:0*/ Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out;
        IData/*31:0*/ Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out;
        IData/*31:0*/ Top__DOT__alu_unit__DOT__AdderResult;
        IData/*31:0*/ Top__DOT__alu_unit__DOT__selAdder__DOT__t_Cin;
        IData/*31:0*/ Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out;
        IData/*31:0*/ __Vtask_ebreak__0__Vfuncout;
        IData/*31:0*/ __VstlIterCount;
        IData/*31:0*/ __VactIterCount;
        VlUnpacked<IData/*31:0*/, 256> Top__DOT__imem_unit__DOT__rom0__DOT__rom_data;
        VlUnpacked<CData/*6:0*/, 3> Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__key_list;
        VlUnpacked<CData/*1:0*/, 3> Top__DOT__controller_unit__DOT__wb_mux__DOT__i0__DOT__data_list;
        VlUnpacked<CData/*6:0*/, 5> Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__key_list;
        VlUnpacked<CData/*2:0*/, 5> Top__DOT__controller_unit__DOT__imm_mux__DOT__i0__DOT__data_list;
        VlUnpacked<CData/*6:0*/, 2> Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__key_list;
        VlUnpacked<CData/*2:0*/, 2> Top__DOT__controller_unit__DOT__alu_sel_mux__DOT__i0__DOT__data_list;
        VlUnpacked<CData/*2:0*/, 6> Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__key_list;
        VlUnpacked<CData/*0:0*/, 6> Top__DOT__controller_unit__DOT__branch_logic_mux__DOT__i0__DOT__data_list;
        VlUnpacked<IData/*31:0*/, 32> Top__DOT__reg_file_unit__DOT__reg_file;
        VlUnpacked<CData/*2:0*/, 6> Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__key_list;
        VlUnpacked<IData/*31:0*/, 6> Top__DOT__imm_gen_unit__DOT__i0__DOT__i0__DOT__data_list;
        VlUnpacked<CData/*0:0*/, 2> Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__key_list;
    };
    struct {
        VlUnpacked<IData/*31:0*/, 2> Top__DOT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__data_list;
        VlUnpacked<CData/*0:0*/, 2> Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__key_list;
        VlUnpacked<IData/*31:0*/, 2> Top__DOT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__data_list;
        VlUnpacked<CData/*2:0*/, 3> Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__key_list;
        VlUnpacked<CData/*0:0*/, 3> Top__DOT__alu_unit__DOT__i0__DOT__i0__DOT__data_list;
        VlUnpacked<IData/*31:0*/, 1024> Top__DOT__dmem_unit__DOT__mem;
        VlUnpacked<CData/*1:0*/, 3> Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__key_list;
        VlUnpacked<IData/*31:0*/, 3> Top__DOT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list;
    };
    VlTriggerVec<1> __VstlTriggered;
    VlTriggerVec<2> __VactTriggered;
    VlTriggerVec<2> __VnbaTriggered;

    // INTERNAL VARIABLES
    VTop__Syms* const vlSymsp;

    // CONSTRUCTORS
    VTop___024root(VTop__Syms* symsp, const char* v__name);
    ~VTop___024root();
    VL_UNCOPYABLE(VTop___024root);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
