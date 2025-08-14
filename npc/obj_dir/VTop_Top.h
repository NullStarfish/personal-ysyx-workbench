// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design internal header
// See VTop.h for the primary calling header

#ifndef VERILATED_VTOP_TOP_H_
#define VERILATED_VTOP_TOP_H_  // guard

#include "verilated.h"

class VTop__Syms;
class VTop_RegFile;


class VTop_Top final : public VerilatedModule {
  public:
    // CELLS
    VTop_RegFile* reg_file_unit;

    // DESIGN SPECIFIC STATE
    VL_IN8(clk,0,0);
    VL_IN8(rst,0,0);
    VL_IN8(load_en,0,0);
    CData/*0:0*/ __PVT__RegWEn;
    CData/*0:0*/ __PVT__DMWen;
    CData/*0:0*/ __PVT__Asel;
    CData/*0:0*/ __PVT__Bsel;
    CData/*0:0*/ __PVT__PCSel;
    CData/*0:0*/ __PVT__BrUn;
    CData/*0:0*/ __PVT__ForceRs1ToZero;
    CData/*1:0*/ __PVT__WBSel;
    CData/*2:0*/ __PVT__ImmSel;
    CData/*3:0*/ __PVT__ALUSel;
    CData/*4:0*/ __PVT__rs1_addr;
    CData/*0:0*/ __PVT__rom_we;
    CData/*6:0*/ __PVT__controller_unit__DOT__opcode;
    CData/*2:0*/ __PVT__controller_unit__DOT__funct3;
    CData/*0:0*/ __PVT__controller_unit__DOT__funct7_5;
    CData/*0:0*/ __PVT__alu_unit__DOT__internal_carry;
    CData/*0:0*/ __PVT__alu_unit__DOT__signed_less_than;
    CData/*0:0*/ __VdfgTmp_hff5a8388__0;
    CData/*6:0*/ __Vtableidx1;
    CData/*0:0*/ __Vdlyvset__dmem_unit__DOT__mem__v0;
    SData/*9:0*/ __Vdlyvdim0__dmem_unit__DOT__mem__v0;
    VL_IN(load_addr,31,0);
    VL_IN(load_data,31,0);
    IData/*31:0*/ pc_out;
    IData/*31:0*/ inst;
    IData/*31:0*/ __PVT__imm_out;
    IData/*31:0*/ __PVT__alu_in_a;
    IData/*31:0*/ __PVT__alu_in_b;
    IData/*31:0*/ __PVT__alu_result;
    IData/*31:0*/ __PVT__dmem_rdata;
    IData/*31:0*/ __PVT__rom_waddr;
    IData/*31:0*/ __PVT__rom_wdata;
    IData/*24:0*/ __Vcellinp__imm_gen_unit__inst_in;
    IData/*31:0*/ __PVT__pc_unit__DOT__PCin;
    IData/*31:0*/ __PVT__alu_unit__DOT__adder_result;
    IData/*31:0*/ __PVT__alu_unit__DOT__shifter_result;
    IData/*31:0*/ __PVT__alu_unit__DOT__adder_unit__DOT__t_Cin;
    IData/*31:0*/ __PVT__alu_unit__DOT__shifter_unit__DOT__stage1;
    IData/*31:0*/ __PVT__alu_unit__DOT__shifter_unit__DOT__stage2;
    IData/*31:0*/ __PVT__alu_unit__DOT__shifter_unit__DOT__stage3;
    IData/*31:0*/ __PVT__alu_unit__DOT__shifter_unit__DOT__stage4;
    IData/*31:0*/ __VdfgTmp_hb5448552__0;
    IData/*31:0*/ __Vdlyvval__dmem_unit__DOT__mem__v0;
    VlUnpacked<IData/*31:0*/, 32768> __PVT__imem_unit__DOT__rom0__DOT__rom_data;
    VlUnpacked<IData/*31:0*/, 1024> __PVT__dmem_unit__DOT__mem;

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
