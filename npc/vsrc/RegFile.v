module RegFile (
    input clk,
    input rst,
    input [31:0] DataD,
    input [4:0] AddrD,
    input [4:0] AddrA,
    input [4:0] AddrB,
    output [31:0] DataA,
    output [31:0] DataB,
    input RegWEn,
    input load_en 
);
    // 将 reg_file 声明为 public，以便 C++ testbench 可以访问
    reg [31:0] reg_file [0:31] /*verilator public*/;

    always @(posedge clk) begin
        if (rst) begin
            // If reset is active, only perform the reset.
            integer i;
            for (i = 0; i < 32; i = i + 1) begin
                reg_file[i] <= 32'b0;
            end
        end 
        // The 'else' ensures this part only runs when rst is low.
        else if (RegWEn && AddrD != 5'b0 && !load_en) begin
            // Only write if not resetting, not loading, and a valid write is enabled.
            $display("RegFile write: time=%0t AddrD=%d DataD=%h", $time, AddrD, DataD);
            reg_file[AddrD] <= DataD;
        end
    end

    assign DataA = (AddrA == 5'b0) ? 32'b0 : reg_file[AddrA];
    assign DataB = (AddrB == 5'b0) ? 32'b0 : reg_file[AddrB];
endmodule
