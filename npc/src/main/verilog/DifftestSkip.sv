module DifftestSkip(
    input  logic        clock,
    input  logic        skip
);
    import "DPI-C" function void difftest_skip_ref_cpp();
    always_ff @(posedge clock) begin
        if (skip) begin
            difftest_skip_ref_cpp();
        end
    end
endmodule