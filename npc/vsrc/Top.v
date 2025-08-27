// Top.v
// Final version: The Loader module has been removed.
// Program loading is now handled entirely on the C++ side.

module Top (
    input clk,
    input rst
);

    // --- Wires for Datapath Connections ---
    wire [31:0] pc_out /*verilator public*/, pc_plus_4;
    wire [31:0] inst   /*verilator public*/;
    wire [2:0]  funct3 = inst[14:12];
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
    wire        BrEq, BrLT;


    wire [31:0] dmem_rdata_raw;
    wire [31:0] dmem_rdata;
    wire [7:0]  wmask; // New write mask wire

    // --- Module Instantiations ---
    PC pc_unit (.clk(clk), .rst(rst), .PCsel(PCSel), .ALUin(alu_result), .pc(pc_out), .PCplus4(pc_plus_4));

    Memory memory_unit (
        .i_addr(pc_out),
        .i_rdata(inst),
        .d_addr(alu_result),
        .d_rdata(dmem_rdata_raw),
        .d_wen(DMWen),
        .wmask(wmask), // New write mask input
        .d_wdata(reg_rs2_data)
    );

    
    LOAD_Decoder u_LOAD_Decoder(
        .raw_addr 	(alu_result  ),
        .raw_data 	(dmem_rdata_raw  ),
        .funct3   	(funct3    ),
        .out      	(dmem_rdata       )
    );
    



    
    STORE_Decoder u_STORE_Decoder(
        .raw_addr 	(alu_result  ),
        .funct3   	(funct3    ),
        .wmask    	(wmask     )
    );
    

    assign rd  = inst[11:7];
    assign rs1_raw = inst[19:15];
    assign rs2 = inst[24:20];

    Controller controller_unit (
        .inst(inst), .BrEq(BrEq), .BrLT(BrLT),
        .RegWEn(RegWEn), .DMWen(DMWen), .Asel(Asel), .Bsel(Bsel),
        .WBSel(WBSel), .ImmSel(ImmSel), .ALUSel(ALUSel),
        .BrUn(BrUn), .PCSel(PCSel), .ForceRs1ToZero(ForceRs1ToZero)
    );
    
    assign rs1_addr = ForceRs1ToZero ? 5'b0 : rs1_raw;

    // The 'load_en' port on RegFile is tied low because loading is now a C++-only operation.
    RegFile reg_file_unit (
        .clk(clk), .rst(rst), .DataD(wb_data), .AddrD(rd), .AddrA(rs1_addr), .AddrB(rs2), 
        .DataA(reg_rs1_data), .DataB(reg_rs2_data), .RegWEn(RegWEn)
    );
    
    ImmGen imm_gen_unit (.inst_in(inst), .ImmSel(ImmSel), .imm_out(imm_out));

    ALUDataIn alu_data_in_unit (.RegRs1(reg_rs1_data), .RegRs2(reg_rs2_data), .Imm(imm_out), .PC(pc_out), .Asel(Asel), .Bsel(Bsel), .A(alu_in_a), .B(alu_in_b));

    ALU alu_unit (
        .A(alu_in_a), .B(alu_in_b), .ALUSel(ALUSel), .Results(alu_result)
    );

    // output declaration of module BranchComp
    wire BrEq;
    wire BrLT;
    
    BranchComp u_BranchComp(
        .rs1  	(reg_rs1_data   ),
        .rs2  	(reg_rs2_data   ),
        .BrUn 	(BrUn  ),
        .BrEq 	(BrEq  ),
        .BrLT 	(BrLT  )
    );
    

    WBMux wb_mux_unit (.ALUResult(alu_result), .DMEMData(dmem_rdata), .PCPlus4(pc_plus_4), .WBSel(WBSel), .WBData(wb_data));

endmodule
