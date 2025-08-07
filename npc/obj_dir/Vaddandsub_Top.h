// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See Vaddandsub.h for the primary calling header

#ifndef VERILATED_VADDANDSUB_TOP_H_
#define VERILATED_VADDANDSUB_TOP_H_  // guard

#include "verilated.h"

class Vaddandsub__Syms;
class Vaddandsub_IMEM;


class Vaddandsub_Top final : public VerilatedModule {
  public:
    // CELLS
    Vaddandsub_IMEM* imem_unit;

    // DESIGN SPECIFIC STATE
    // Anonymous structures to workaround compiler member-count bugs
    struct {
        VL_IN8(clk,0,0);
        VL_IN8(rst,0,0);
        CData/*6:0*/ __PVT__opcode;
        CData/*0:0*/ __PVT__Asel;
        CData/*0:0*/ __PVT__Bsel;
        CData/*0:0*/ __PVT__BrUn;
        CData/*1:0*/ __PVT__WBSel;
        CData/*2:0*/ __PVT__ImmSel;
        CData/*2:0*/ __PVT__ALUSel;
        CData/*4:0*/ __PVT__rs1_addr;
        CData/*0:0*/ __PVT__alu_br_lt;
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
        CData/*0:0*/ __PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__hit;
        CData/*0:0*/ __PVT__alu_unit__DOT__AdderCtrl;
        CData/*0:0*/ __PVT__alu_unit__DOT__internal_overflow;
        CData/*0:0*/ __PVT__alu_unit__DOT__internal_carry;
        CData/*0:0*/ __PVT__alu_unit__DOT__i0__DOT__i0__DOT__lut_out;
        CData/*0:0*/ __PVT__alu_unit__DOT__i0__DOT__i0__DOT__hit;
        CData/*0:0*/ __PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__hit;
        CData/*1:0*/ __VdfgTmp_h9f39adc8__0;
        IData/*31:0*/ __PVT__pc_out;
        IData/*31:0*/ __PVT__reg_rs2_data;
        IData/*31:0*/ __PVT__wb_data;
        IData/*31:0*/ __PVT__imm_out;
        IData/*31:0*/ __PVT__alu_in_a;
        IData/*31:0*/ __PVT__alu_in_b;
        IData/*31:0*/ __PVT__alu_result;
        IData/*31:0*/ __PVT__dmem_rdata;
        IData/*31:0*/ __PVT__pc_unit__DOT__PCin;
        IData/*31:0*/ __PVT__imm_gen_unit__DOT__i0__DOT__i0__DOT__lut_out;
        IData/*31:0*/ __PVT__alu_data_in_unit__DOT__i0__DOT__i0__DOT__lut_out;
        IData/*31:0*/ __PVT__alu_data_in_unit__DOT__i1__DOT__i0__DOT__lut_out;
        IData/*31:0*/ __PVT__alu_unit__DOT__AdderResult;
        IData/*31:0*/ __PVT__alu_unit__DOT__selAdder__DOT__t_Cin;
        IData/*31:0*/ __PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__lut_out;
        IData/*31:0*/ __Vtask_ebreak__0__Vfuncout;
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
    };
    struct {
        VlUnpacked<IData/*31:0*/, 3> __PVT__wb_mux_unit__DOT__i0__DOT__i0__DOT__data_list;
    };

    // INTERNAL VARIABLES
    Vaddandsub__Syms* const vlSymsp;

    // CONSTRUCTORS
    Vaddandsub_Top(Vaddandsub__Syms* symsp, const char* v__name);
    ~Vaddandsub_Top();
    VL_UNCOPYABLE(Vaddandsub_Top);

    // INTERNAL METHODS
    void __Vconfigure(bool first);
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);


#endif  // guard
