// ALU.sv
import cpu_types_pkg::*;
import exu_types_pkg::*;

module ALU #(parameter WIDTH = 32) (
    // input  logic [WIDTH-1:0] A,
    // input  logic [WIDTH-1:0] B,
    // input  alusel_e          ALUSel,

    input  logic             clk,
    // input  logic             valid,
    // output  logic             ready,//i am slave
    input logic             rst,
    stage_if.slave          alu_in,
    stage_if.master         alu_out

    //output logic [WIDTH-1:0] Results
);

    wire [31:0] A = alu_in.payload.dataA;
    wire [31:0] B = alu_in.payload.dataB;
    alusel_e ALUSel = alu_in.payload.opcode;

    logic [WIDTH - 1 : 0] Results;
    assign alu_out.payload = Results;





    // ADDAndSub
    // Internal signals for functional units
    logic [WIDTH-1:0] adder_result;

    logic             internal_carry;
    logic             internal_overflow;
    logic             internal_zero; // Wire to connect the zero output
    logic             When2Sub;



    assign When2Sub = ALUSel == ALU_SUB || ALUSel == ALU_SLT || ALUSel == ALU_SLTU;
    AddAndSub #(.WIDTH(WIDTH)) adder_unit (
        .A(A),
        .B(B),
        .Cin(When2Sub),
        .Carry(internal_carry),
        .Overflow(internal_overflow),
        .Result(adder_result),
        .zero(internal_zero)
    );




    // Instantiate Shifter unit
    logic [1:0] shift_type_internal;
    logic [WIDTH-1:0] shifter_result;
    always_comb begin
        unique case (ALUSel)
            ALU_SLL: shift_type_internal = 2'b00; // SLL
            ALU_SRL: shift_type_internal = 2'b01; // SRL
            ALU_SRA: shift_type_internal = 2'b10; // SRA
            default: shift_type_internal = 2'bxx; // Don't care for non-shift operations
        endcase
    end


    BarrelShifter shifter_unit (
        .data_in(A),
        .shamt(B[4:0]),
        .shift_type(shift_type_internal),
        .data_out(shifter_result)
    );



    // Comparison logic
    logic signed_less_than   = internal_overflow ^ adder_result[WIDTH-1];
    logic unsigned_less_than = ~internal_carry;



    stage_if #(alu_div_t) alu_div_if();
    stage_if #()          div_alu_if();
    alu_div_t             div_payload_reg;
    assign alu_div_if.payload = div_payload_reg;


    //we also need translate alusel_e to riscv_div_op_e
    riscv_div_op_e div_op;
    always_comb begin
        div_op = DIV_NONE;
        case (ALUSel) 
            ALU_DIV: div_op = DIV;
            ALU_DIVU: div_op = DIVU;
            ALU_REM: div_op = REM;
            ALU_REMU: div_op = REMU;
            default:;
        endcase  
    end


    OptimizedDivider u_div (
        .clk    (clk),
        .rst    (rst),
        .div_in (alu_div_if.slave),
        .div_out (div_alu_if.master)
    );







    // Final result selection

    logic is_seq = (ALUSel == ALU_MUL    ||
                    ALUSel == ALU_MULH   ||
                    ALUSel == ALU_MULHSU ||
                    ALUSel == ALU_MULHU  ||
                    ALUSel == ALU_DIV    ||
                    ALUSel == ALU_DIVU   ||
                    ALUSel == ALU_REM    ||
                    ALUSel == ALU_REMU   );




    always_comb begin
        unique case (ALUSel)
            ALU_ADD:  begin  Results = adder_result;end
            ALU_SUB:  Results = adder_result;
            ALU_AND:  Results = A & B;
            ALU_OR:   Results = A | B;
            ALU_XOR:  Results = A ^ B;
            ALU_SLT:  Results = {{(WIDTH-1){1'b0}}, signed_less_than};
            ALU_SLTU: Results = {{(WIDTH-1){1'b0}}, unsigned_less_than};
            ALU_SLL:  Results = shifter_result;
            ALU_SRL:  Results = shifter_result;
            ALU_SRA:  Results = shifter_result;
            ALU_COPY_A:  Results = A;
            ALU_COPY_B:  Results = B;
            ALU_DIV, ALU_DIVU, ALU_REM, ALU_REMU: Results = div_alu_if.payload;
            default:  Results = 32'hdeadbeef;
        endcase
    end


    //----------------------------------------------------------------
    // Moore FSM Handshake Logic
    //----------------------------------------------------------------
    typedef enum logic [1:0] {
        S_IDLE, // 等待 EXU 发起请求
        S_WAIT_SEQU,
        S_CALC,  // 正在进行计算 (对于组合ALU，此状态持续一个周期)
        S_WAIT_EXU
    } alu_state_e;
    
    alu_state_e cur_state, next_state;

    // 状态寄存器
    always_ff @(posedge clk) begin
        if (rst) begin
            cur_state <= S_IDLE;
        end else begin
            cur_state <= next_state;
        end
    end



    //时序输出逻辑
    always_ff @(posedge clk) begin
        if (rst) begin
            div_payload_reg.dataA <= 0;
            div_payload_reg.dataB <= 0;
            div_payload_reg.opcode <= DIV_NONE;
        end else begin
            if (cur_state == S_IDLE && next_state == S_WAIT_SEQU) begin
                div_payload_reg.dataA <= alu_in.payload.dataA;
                div_payload_reg.dataB <= alu_in.payload.dataB;
                div_payload_reg.opcode <= div_op;
            end
        end
    end


    // 状态转移逻辑 (组合逻辑)
    always_comb begin
        next_state = cur_state;
        unique case (cur_state)
            S_IDLE: begin
                // 如果 EXU 发来了有效的请求，则下一个周期进入计算状态

                if (alu_in.fire) begin
                    if (is_seq) 
                        next_state = S_WAIT_SEQU;
                    else
                        next_state = S_WAIT_EXU;
                end

            end
            S_WAIT_SEQU: begin
                if (alu_div_if.fire)
                    next_state = S_CALC;
            end
            S_CALC: begin
                if (div_alu_if.fire)
                    next_state = S_WAIT_EXU;
            end
            S_WAIT_EXU: begin
                if (alu_out.fire) begin
                    next_state = S_IDLE;
                end
            end
        endcase
    end

    // 输出逻辑 (组合逻辑, Moore 类型)
    // ready 信号只依赖于当前状态 cur_state
    always_comb begin
        alu_in.ready = 1'b0; // 默认情况下，ALU 没有准备好
        alu_out.valid = 1'b0;
        alu_div_if.valid = 1'b0;
        div_alu_if.ready = 1'b0;
        unique case (cur_state)
            S_IDLE: begin
                alu_in.ready = 1'b1;
            end
            S_WAIT_SEQU:begin
                alu_div_if.valid = 1'b1;
            end
            S_CALC: begin
                div_alu_if.ready = 1'b0;
            end
            S_WAIT_EXU: begin
                alu_out.valid = 1'b1;
            end
        endcase
    end

endmodule

