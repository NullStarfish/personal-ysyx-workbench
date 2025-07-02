
// Top_updated.v
// Top-level module for the RISC-V CPU with advanced branch support.
module Top (
    input clk,
    input rst
);

    // --- Wires for Datapath Connections ---
    wire [31:0] pc_out, pc_plus_4;
    wire [31:0] inst;
    wire [6:0]  opcode;
    wire [4:0]  rd, rs1_raw, rs2;
    wire [2:0]  funct3;
    wire [6:0]  funct7;
    wire        RegWEn, DMWen, Asel, Bsel, PCSel, BrUn;
    wire [1:0]  WBSel;
    wire [2:0]  ImmSel, ALUSel;
    wire [4:0]  rs1_addr;
    wire [31:0] reg_rs1_data, reg_rs2_data, wb_data;
    wire [31:0] imm_out;
    wire [31:0] alu_in_a, alu_in_b, alu_result;
    wire        alu_br_eq, alu_br_lt; // New branch flags from ALU
    wire [31:0] dmem_rdata;

    // --- Module Instantiations ---

    PC pc_unit (.clk(clk), .rst(rst), .PCsel(PCSel), .ALUin(alu_result), .PC(pc_out), .PCplus4(pc_plus_4));

    IMEM imem_unit (.addr(pc_out), .inst(inst), .rd(rd), .rs1(rs1_raw), .rs2(rs2));

    Controller controller_unit (
        .inst(inst), // Pass the full instruction to the controller
        .BrEq(alu_br_eq), .BrLT(alu_br_lt), // Connect new flags
        .RegWEn(RegWEn), .DMWen(DMWen), .Asel(Asel), .Bsel(Bsel),
        .WBSel(WBSel), .ImmSel(ImmSel), .ALUSel(ALUSel),
        .BrUn(BrUn), // Connect new unsigned select
        .PCSel(PCSel)
    );
    
    assign rs1_addr = (opcode == 7'b0110111 || opcode == 7'b0010111) ? 5'b00000 : rs1_raw;

    RegFile reg_file_unit (.clk(clk), .rst(rst), .DataD(wb_data), .AddrD(rd), .AddrA(rs1_addr), .AddrB(rs2), .DataA(reg_rs1_data), .DataB(reg_rs2_data), .RegWEn(RegWEn));

    ImmGen imm_gen_unit (.inst_in(inst[31:7]), .ImmSel(ImmSel), .imm_out(imm_out));

    ALUDataIn alu_data_in_unit (.RegRs1(reg_rs1_data), .RegRs2(reg_rs2_data), .Imm(imm_out), .PC(pc_out), .Asel(Asel), .Bsel(Bsel), .A(alu_in_a), .B(alu_in_b));

    // Instantiate the updated ALU
    ALU alu_unit (
        .A(alu_in_a), .B(alu_in_b), .Mode(ALUSel), 
        .CompUn(BrUn), // Connect unsigned select
        .Results(alu_result),
        .BrEq(alu_br_eq), // Connect new branch flags
        .BrLT(alu_br_lt)
    );

    DMEM dmem_unit (.clk(~clk)/*为了在一个在周期内完成*/, .rst(rst), .addr(alu_result), .wdata(reg_rs2_data), .we(DMWen), .rdata(dmem_rdata));

    WBMux wb_mux_unit (.ALUResult(alu_result), .DMEMData(dmem_rdata), .PCPlus4(pc_plus_4), .WBSel(WBSel), .WBData(wb_data));

endmodule
