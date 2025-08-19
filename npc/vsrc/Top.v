// Top.v
// FINAL CORRECTED: This module is now clean of any DPI-C details.

module Top (
    input clk,
    input rst,
    input load_en,
    input [31:0] load_addr,
    input [31:0] load_data
);

    // --- Wires for Datapath Connections ---
    wire [31:0] pc_out /*verilator public*/, pc_plus_4;
    wire [31:0] inst   /*verilator public*/;
    wire [4:0]  rd, rs1_raw, rs2;
    wire        RegWEn, DMWen, Asel, Bsel, PCSel, BrUn;
    wire        ForceRs1ToZero;
    wire [1:0]  WBSel;
    wire [2:0]  ImmSel;
    wire [3:0]  ALUSel;
    wire [4:0]  rs1_addr;
    wire [31:0] reg_rs1_data, reg_rs2_data, wb_data;
    wire [31:0] imm_out;
    wire [31:0] alu_in_a, alu_in_b, alu_result;
    wire        alu_br_eq, alu_br_lt;
    wire [31:0] dmem_rdata;
    wire        rom_we;
    wire [31:0] rom_waddr;
    wire [31:0] rom_wdata;

    // --- Module Instantiations ---
    Loader loader_unit (
        .clk(clk), .rst(rst), .load_en(load_en), .load_addr(load_addr), .load_data(load_data),
        .rom_we(rom_we), .rom_waddr(rom_waddr), .rom_wdata(rom_wdata)
    );

    PC pc_unit (.clk(clk), .rst(rst), .PCsel(PCSel), .ALUin(alu_result), .pc(pc_out), .PCplus4(pc_plus_4));

    IMEM imem_unit (
        .clk(clk), .addr(pc_out), .inst(inst), .rd(rd), .rs1(rs1_raw), .rs2(rs2),
        .rom_we(rom_we), .rom_waddr(rom_waddr), .rom_wdata(rom_wdata)
    );

    Controller controller_unit (
        .inst(inst), .BrEq(alu_br_eq), .BrLT(alu_br_lt),
        .RegWEn(RegWEn), .DMWen(DMWen), .Asel(Asel), .Bsel(Bsel),
        .WBSel(WBSel), .ImmSel(ImmSel), .ALUSel(ALUSel),
        .BrUn(BrUn), .PCSel(PCSel), .ForceRs1ToZero(ForceRs1ToZero)
    );
    
    assign rs1_addr = ForceRs1ToZero ? 5'b0 : rs1_raw;

    RegFile reg_file_unit (.clk(clk), .rst(rst), .DataD(wb_data), .AddrD(rd), .AddrA(rs1_addr), .AddrB(rs2), .DataA(reg_rs1_data), .DataB(reg_rs2_data), .RegWEn(RegWEn), .load_en(load_en));

   // 在 Top.v 中
// 错误的方式: .inst_in(inst[31:7])
// 正确的方式:
    ImmGen imm_gen_unit (.inst_in(inst), .ImmSel(ImmSel), .imm_out(imm_out));

    ALUDataIn alu_data_in_unit (.RegRs1(reg_rs1_data), .RegRs2(reg_rs2_data), .Imm(imm_out), .PC(pc_out), .Asel(Asel), .Bsel(Bsel), .A(alu_in_a), .B(alu_in_b));

    ALU alu_unit (
        .A(alu_in_a), .B(alu_in_b), .ALUSel(ALUSel),
        .CompUn(BrUn), .Results(alu_result),
        .BrEq(alu_br_eq), .BrLT(alu_br_lt)
    );

    DMEM dmem_unit (.clk(~clk), .rst(rst), .addr(alu_result), .wdata(reg_rs2_data), .we(DMWen), .rdata(dmem_rdata));

    WBMux wb_mux_unit (.ALUResult(alu_result), .DMEMData(dmem_rdata), .PCPlus4(pc_plus_4), .WBSel(WBSel), .WBData(wb_data));

endmodule
