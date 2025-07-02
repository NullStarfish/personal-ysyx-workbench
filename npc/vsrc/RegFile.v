module RegFile (
    input clk,
    input rst, // reset signal
    input [31:0] DataD,
    input [4:0] AddrD,
    input [4:0] AddrA,
    input [4:0] AddrB,
    output [31:0] DataA,
    output [31:0] DataB,
    input RegWEn //control signal to write to the register file
);
    reg [31:0] reg_file [0:31]; //32 registers of 32 bits each

    always @(posedge clk) begin
        if (rst) begin
            integer i;
            for (i = 0; i < 32; i = i + 1) begin
                reg_file[i] <= 32'b0;
            end
        end
        if (RegWEn && AddrD != 5'b00000) begin
            $display("RegFile write: time=%0t AddrD=%d DataD=%h", $time, AddrD, DataD);
            reg_file[AddrD] <= DataD;
        end
    end

    assign DataA = (AddrA == 5'b00000) ? 32'b0 : reg_file[AddrA]; // Read register A, return 0 if AddrA is zero
    assign DataB = (AddrB == 5'b00000) ? 32'b0 : reg_file[AddrB]; // Read register B, return 0 if AddrB is zero
    // Note: Register 0 (AddrD = 5'b00000) is hardwired to zero, so it cannot be written to.
    // This is a common feature in RISC architectures to simplify the design and avoid unnecessary checks
endmodule