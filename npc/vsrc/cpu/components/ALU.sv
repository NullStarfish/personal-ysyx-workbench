// ALU.sv
import cpu_types_pkg::*;

module ALU #(parameter WIDTH = 32) (
    input  logic [WIDTH-1:0] A,
    input  logic [WIDTH-1:0] B,
    input  alusel_e          ALUSel,

    input  logic             clk,
    input  logic             valid,
    output  logic             ready,//i am slave
    input logic             rst,

    output logic [WIDTH-1:0] Results
);
    // Internal signals for functional units
    logic [WIDTH-1:0] adder_result;
    logic [WIDTH-1:0] shifter_result;
    logic             internal_carry;
    logic             internal_overflow;
    logic             internal_zero; // Wire to connect the zero output
    logic             When2Sub;

    // Instantiate Add/Sub unit
    // MODIFIED: Connected the .zero port to resolve PINMISSING warning.


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
    BarrelShifter shifter_unit (
        .data_in(A),
        .shamt(B[4:0]),
        .shift_type(ALUSel[1:0]),
        .data_out(shifter_result)
    );

    // Comparison logic
    logic signed_less_than   = internal_overflow ^ adder_result[WIDTH-1];
    logic unsigned_less_than = ~internal_carry;

    // Final result selection
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
            default:  Results = 32'hdeadbeef;
        endcase
    end


    //----------------------------------------------------------------
    // Moore FSM Handshake Logic
    //----------------------------------------------------------------
    typedef enum logic {
        S_IDLE, // 等待 EXU 发起请求
        S_CALC  // 正在进行计算 (对于组合ALU，此状态持续一个周期)
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

    // 状态转移逻辑 (组合逻辑)
    always_comb begin
        next_state = cur_state;
        unique case (cur_state)
            S_IDLE: begin
                // 如果 EXU 发来了有效的请求，则下一个周期进入计算状态
                if (valid) begin
                    next_state = S_CALC;
                end
            end
            S_CALC: begin
                // 计算状态只持续一个周期，然后无条件返回IDLE
                // (如果未来有真正的多周期操作，这里的条件会变为 `if (real_done)`)
                next_state = S_IDLE;
            end
        endcase
    end

    // 输出逻辑 (组合逻辑, Moore 类型)
    // ready 信号只依赖于当前状态 cur_state
    always_comb begin
        ready = 1'b0; // 默认情况下，ALU 没有准备好
        unique case (cur_state)
            S_IDLE: begin
                // 在IDLE状态，ALU 正在等待任务，计算结果尚未就绪
                ready = 1'b0;
            end
            S_CALC: begin
                // 只要进入了CALC状态，就意味着上一个周期的计算已经完成，
                // 结果在这个周期是有效的。因此将 ready 置为1，通知EXU来取结果。
                ready = 1'b1;
            end
        endcase
    end

endmodule

