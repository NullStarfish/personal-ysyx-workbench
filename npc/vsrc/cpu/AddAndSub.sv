// AddAndSub.sv
// SystemVerilog version of the Adder/Subtractor unit.

module AddAndSub #(
    parameter WIDTH = 32
) (
    input  logic [WIDTH-1:0] A,
    input  logic [WIDTH-1:0] B,
    input  logic             Cin,      // 1 for subtract, 0 for add
    output logic             Carry,
    output logic             zero,
    output logic             Overflow,
    output logic [WIDTH-1:0] Result
);
    logic [WIDTH-1:0] b_inv;
    logic [WIDTH:0]   sum;

    assign b_inv = B ^ {WIDTH{Cin}};
    // MODIFIED: Reverted to the simple, context-determined addition.
    // Verilog correctly handles the carry-in for this operation.
    // The explicit casting was causing more issues than it solved.
    assign sum = A + b_inv + Cin;

    assign Result = sum[WIDTH-1:0];
    assign Carry = sum[WIDTH];

    // Overflow for signed numbers
    assign Overflow = (A[WIDTH-1] == b_inv[WIDTH-1]) && (Result[WIDTH-1] != A[WIDTH-1]);

    // Zero flag
    assign zero = ~|Result;

endmodule

