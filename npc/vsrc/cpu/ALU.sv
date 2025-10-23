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

    exu_alu_t alu_in_payload_reg;

    wire [31:0] A;
    wire [31:0] B; 
    alusel_e ALUSel;
    assign A = alu_in_payload_reg.dataA;
    assign B = alu_in_payload_reg.dataB;
    assign ALUSel = alu_in_payload_reg.opcode;


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

    assign alu_div_if.payload.opcode = div_op;


    OptimizedDivider u_div (
        .clk    (clk),
        .rst    (rst),
        .div_in (alu_div_if.slave),
        .div_out (div_alu_if.master)
    );


    stage_if #(alu_mul_t) alu_mul_if();
    stage_if              mul_alu_if();
    alu_mul_t            mul_payload_reg;
    assign alu_mul_if.payload = mul_payload_reg;


    riscv_mul_op_e mul_op;
    always_comb begin
        mul_op = MUL_NONE;
        case (ALUSel) 
            ALU_MUL: mul_op = MUL;
            ALU_MULH: mul_op = MULH;
            ALU_MULHSU: mul_op = MULHSU;
            ALU_MULHU: mul_op = MULHU;
            default:;
        endcase  
    end

    assign alu_mul_if.payload.opcode = mul_op;


    BoothMultiplier u_mul (
        .clk (clk),
        .rst (rst),
        .mul_in (alu_mul_if.slave),
        .mul_out (mul_alu_if.master)
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


    logic is_mul = (ALUSel == ALU_MUL    ||
                    ALUSel == ALU_MULH   ||
                    ALUSel == ALU_MULHSU ||
                    ALUSel == ALU_MULHU );

    
    logic is_div = (ALUSel == ALU_DIV    ||
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
            ALU_MUL, ALU_MULH, ALU_MULHSU, ALU_MULHU: Results = mul_alu_if.payload;
            default:  Results = 32'hdeadbeef;
        endcase
    end


    //----------------------------------------------------------------
    // Moore FSM Handshake Logic
    //----------------------------------------------------------------
    typedef enum logic [2:0] {
        S_IDLE, // 等待 EXU 发起请求
        S_PREP,
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

            mul_payload_reg.dataA <= 0;
            mul_payload_reg.dataB <= 0;
            mul_payload_reg.opcode <= MUL_NONE;



            alu_in_payload_reg.dataA <= 0;
            alu_in_payload_reg.dataB <= 0;
            alu_in_payload_reg.opcode <= ALU_NOP;

        end else begin
            if (cur_state == S_IDLE && next_state == S_PREP) begin
                div_payload_reg.dataA <= alu_in.payload.dataA;
                div_payload_reg.dataB <= alu_in.payload.dataB;
                div_payload_reg.opcode <= div_op;

                mul_payload_reg.dataA <= alu_in.payload.dataA;
                mul_payload_reg.dataB <= alu_in.payload.dataB;
                mul_payload_reg.opcode <= mul_op;

                alu_in_payload_reg.dataA <= alu_in.payload.dataA;
                alu_in_payload_reg.dataB <= alu_in.payload.dataB;
                alu_in_payload_reg.opcode <= alu_in.payload.opcode;

            end

        end
        if (cur_state == S_PREP && next_state == S_WAIT_SEQU) begin
            $display("begin div or mul");
            if (is_mul)
                $display("data: a: %d, b: %d, op: %d", alu_mul_if.payload.dataA, alu_mul_if.payload.dataB, alu_mul_if.payload.opcode);
            else
                $display("data: a: %d, b: %d, op: %d", alu_div_if.payload.dataA, alu_div_if.payload.dataB, alu_div_if.payload.opcode);

        end
        if (cur_state == S_WAIT_SEQU && next_state == S_CALC) begin
            $display("mul or div begin calc");
        end
        if (cur_state == S_CALC && next_state == S_WAIT_EXU) begin
            if (is_mul)
                $display("mul results %d", mul_alu_if.payload);
            else
                $display("div results %d", div_alu_if.payload); 
        end
    end


    // 状态转移逻辑 (组合逻辑)
    always_comb begin
        next_state = cur_state;
        unique case (cur_state)
            S_IDLE: begin
                // 如果 EXU 发来了有效的请求，则下一个周期进入计算状态

                if (alu_in.fire) begin
                    next_state = S_PREP;
                end

            end
            S_PREP: begin
                if (is_seq) 
                    next_state = S_WAIT_SEQU;
                else
                    next_state = S_WAIT_EXU;
            end
            S_WAIT_SEQU: begin//fire由alu调控
                if (alu_div_if.fire || alu_mul_if.fire)
                    next_state = S_CALC;
            end
            S_CALC: begin
                if (div_alu_if.fire || mul_alu_if.fire)
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

        alu_mul_if.valid = 1'b0;
        mul_alu_if.ready = 1'b0;
        unique case (cur_state)
            S_IDLE: begin
                //$display("alu idle");
                alu_in.ready = 1'b1;
            end
            S_WAIT_SEQU:begin
                if (is_div) begin
                    //$display("begin div");
                    alu_div_if.valid = 1'b1;
                end
                else if (is_mul)
                    alu_mul_if.valid = 1'b1;
            end
            S_CALC: begin
                if (is_div) begin
                    //$display("begin div calc");
                    div_alu_if.ready = 1'b1;
                end
                else if (is_mul)
                    mul_alu_if.ready = 1'b1;
            end
            S_WAIT_EXU: begin
                
                alu_out.valid = 1'b1;
                //$display("Results = %d", Results);
            end
            default:;
        endcase
    end

endmodule

