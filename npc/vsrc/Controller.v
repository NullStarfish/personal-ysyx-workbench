// Controller.v
// 优化后：增加 ForceRs1ToZero 信号，将逻辑集中化

`include "Opcodes.v"

module Controller (
    input [31:0] inst,
    input BrEq,
    input BrLT,

    output reg RegWEn,
    output reg DMWen,
    output reg Asel,
    output reg Bsel,
    output reg [1:0] WBSel,
    output reg [2:0] ImmSel,
    output reg [3:0] ALUSel,
    output reg BrUn,
    output reg PCSel,
    output reg ForceRs1ToZero// 新增：用于强制 rs1 地址为 0
);

    wire [6:0] opcode = inst[6:0];
    wire [2:0] funct3 = inst[14:12];
    wire       funct7_5 = inst[30];
    
    import "DPI-C" function void ebreak();
    // LOGIC BLOCK 1: Datapath Control Signals
    always @(*) begin
        // --- 默认值 ---
        RegWEn  = 1'b0;
        DMWen   = 1'b0;
        Asel    = 1'b0;
        Bsel    = 1'b0;
        WBSel   = `WB_ALU;
        ImmSel  = `IMM_I;
        ALUSel  = `ALU_ADD;
        BrUn    = 1'b0;
        ForceRs1ToZero = 1'b0; // 默认不强制

        case (opcode)
            `OPCODE_LUI: begin
                RegWEn = 1'b1;
                Bsel   = 1'b1;
                ImmSel = `IMM_U;
                ForceRs1ToZero = 1'b1; // LUI 指令强制 rs1 为 x0
            end
            `OPCODE_AUIPC: begin
                RegWEn = 1'b1;
                Asel   = 1'b1;
                Bsel   = 1'b1;
                ImmSel = `IMM_U;
                ForceRs1ToZero = 1'b1; // AUIPC 指令也强制 rs1 为 x0
            end
            `OPCODE_JAL: begin
                RegWEn = 1'b1; Asel = 1'b1; Bsel = 1'b1; WBSel = `WB_PC4; ImmSel = `IMM_J;

            end
            `OPCODE_JALR: begin
                RegWEn = 1'b1; Bsel = 1'b1; WBSel = `WB_PC4; ImmSel = `IMM_I;
            end
            `OPCODE_BRANCH: begin
                Asel = 1'b1; Bsel = 1'b1; ImmSel = `IMM_B; ALUSel = `ALU_ADD;
                if (funct3 == `FUNCT3_BLTU || funct3 == `FUNCT3_BGEU) BrUn = 1'b1;
            end
            `OPCODE_LOAD: begin
                RegWEn = 1'b1; Bsel = 1'b1; WBSel = `WB_MEM; ImmSel = `IMM_I; ALUSel = `ALU_ADD;

            end
            `OPCODE_STORE: begin
                DMWen = 1'b1; Bsel = 1'b1; ImmSel = `IMM_S; ALUSel = `ALU_ADD;
            end
            `OPCODE_I_TYPE: begin
                Asel = 1'b0; RegWEn = 1'b1; Bsel = 1'b1; ImmSel = `IMM_I;
                case(funct3)
                    `FUNCT3_ADDI_ADD:   ALUSel = `ALU_ADD;
                    `FUNCT3_SLTI_SLT:   ALUSel = `ALU_SLT;
                    `FUNCT3_SLTIU_SLTU: ALUSel = `ALU_SLTU;
                    `FUNCT3_XORI_XOR:   ALUSel = `ALU_XOR;
                    `FUNCT3_ORI_OR:     ALUSel = `ALU_OR;
                    `FUNCT3_ANDI_AND:   ALUSel = `ALU_AND;
                    `FUNCT3_SLLI_SLL:   ALUSel = `ALU_SLL;
                    `FUNCT3_SRLI_SRAI:  ALUSel = funct7_5 ? `ALU_SRA : `ALU_SRL;
                    default:           ALUSel = `ALU_ADD;
                endcase
            end
            `OPCODE_R_TYPE: begin
                RegWEn = 1'b1;
                case(funct3)
                    `FUNCT3_ADDI_ADD:   ALUSel = funct7_5 ? `ALU_SUB : `ALU_ADD;
                    `FUNCT3_SLLI_SLL:   ALUSel = `ALU_SLL;
                    `FUNCT3_SLTI_SLT:   ALUSel = `ALU_SLT;
                    `FUNCT3_SLTIU_SLTU: ALUSel = `ALU_SLTU;
                    `FUNCT3_XORI_XOR:   ALUSel = `ALU_XOR;
                    `FUNCT3_SRLI_SRAI:  ALUSel = funct7_5 ? `ALU_SRA : `ALU_SRL;
                    `FUNCT3_ORI_OR:     ALUSel = `ALU_OR;
                    `FUNCT3_ANDI_AND:   ALUSel = `ALU_AND;
                    default:           ALUSel = `ALU_ADD;
                endcase
            end
            `OPCODE_I_TYPE_SYS: begin
                // ecall 和 ebreak 都被视为 trap 指令
                if (inst == 32'h00000073 || inst == 32'h00100073) begin
                    ebreak();
                end
            end
            default: begin end
        endcase
    end

    // LOGIC BLOCK 2: PC Selection Logic
    always @(*) begin
        PCSel = 1'b0;
        if (opcode == `OPCODE_JAL || opcode == `OPCODE_JALR) begin
            PCSel = 1'b1;
        end else if (opcode == `OPCODE_BRANCH) begin
            case (funct3)
                `FUNCT3_BEQ:  PCSel = BrEq;
                `FUNCT3_BNE:  PCSel = ~BrEq;
                `FUNCT3_BLT:  PCSel = BrLT;
                `FUNCT3_BGE:  PCSel = ~BrLT;
                `FUNCT3_BLTU: PCSel = BrLT;
                `FUNCT3_BGEU: PCSel = ~BrLT;
                default:      PCSel = 1'b0;
            endcase
        end
    end

endmodule
