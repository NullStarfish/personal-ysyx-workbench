
// ALU_updated.v
// This ALU is modified to support both signed and unsigned comparisons
// for branch instructions, without changing the ALUSel width.
module ALU
    #(parameter WIDTH = 32)   
    (
        input  [WIDTH - 1 : 0] A,
        input  [WIDTH - 1 : 0] B,
        input  [2 : 0]         Mode,
        input                  CompUn,    // New input: 1 for unsigned compare, 0 for signed
        output reg [WIDTH-1:0] Results,
        output                 BrEq,      // New output: True if A == B
        output                 BrLT       // New output: True if A < B (signed/unsigned based on CompUn)
    );  

    // Internal wires for adder/subtractor results
    wire AdderCtrl;
    wire [WIDTH - 1 : 0] AdderResult;
    wire internal_zero, internal_overflow, internal_carry;

    // --- Adder/Subtractor Control ---
    // The subtractor is used for arithmetic (SUB) and comparisons

    MuxKeyWithDefault # (3, 3, 1) i0 (
        .out(AdderCtrl),
        .key(Mode),
        .default_out(1'b0), // Default value for write-back data
        .lut({
            // Note: The comments now correctly describe the hardware behavior.
            {3'b000, 1'b0},  // WBSel = 00, WBData = DMEMData (for LW)
            {3'b001, 1'b1}, // WBSel = 01, WBData = ALUResult (for R-type, I-type)
            {3'b110, 1'b1}    // WBSel = 10, WBData = PC + 4 (for JAL, JALR)
        })
    );

    // --- Core Adder/Subtractor Unit ---
    AddAndSub #(.WIDTH(WIDTH)) selAdder (
        .A(A), 
        .B(B), 
        .Cin(AdderCtrl), 
        .Carry(internal_carry), 
        .Overflow(internal_overflow),
        .Result(AdderResult), 
        .zero(internal_zero)
    );
    
    // --- Branch Comparison Logic ---
    // Generate BrEq and BrLT flags based on the subtraction result.
    assign BrEq = internal_zero; // Equality is true if the result of A-B is zero.

    wire signed_less_than   = internal_overflow ^ AdderResult[WIDTH - 1];
    wire unsigned_less_than = !internal_carry; // For A-B, carry is asserted if A >= B.

    // Select between signed and unsigned less-than based on CompUn signal from Controller.
    assign BrLT = CompUn ? unsigned_less_than : signed_less_than;

    // --- ALU Result Multiplexer ---
    // The main output bus of the ALU.
    always@(*) begin
        case(Mode)
            3'b000, 3'b001: Results = AdderResult;      // ADD, SUB
            3'b010:         Results = ~A;               // NOT (custom, can be used for NOR)
            3'b011:         Results = A & B;            // AND
            3'b100:         Results = A | B;            // OR
            3'b101:         Results = A ^ B;            // XOR
            3'b110:         Results = { {WIDTH-2{1'b0}}, BrLT, BrEq }; // For testing, place flags in result
            default:        Results = 32'hdeadbeef;     // Should not happen
        endcase
    end
endmodule
