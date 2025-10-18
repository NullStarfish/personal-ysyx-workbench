// RegFile.sv

module RegFile (
    input  logic clk,
    input  logic rst,
    input  logic [31:0] DataD,
    input  logic [4:0]  AddrD,
    input  logic [4:0]  AddrA,
    input  logic [4:0]  AddrB,
    output logic [31:0] DataA,
    output logic [31:0] DataB,
    input  logic        RegWEn
);
    // Public for C++ testbench access
    logic [31:0] reg_file [0:31] /* verilator public */;

    always_ff @(posedge clk) begin
        if (rst) begin
            for (int i = 0; i < 32; i++) begin
                reg_file[i] <= 32'b0;
            end
        end else if (RegWEn && AddrD != 5'b0) begin
            reg_file[AddrD] <= DataD;
        end
    end

    assign DataA = (AddrA == 5'b0) ? 32'b0 : reg_file[AddrA];
    assign DataB = (AddrB == 5'b0) ? 32'b0 : reg_file[AddrB];

endmodule
