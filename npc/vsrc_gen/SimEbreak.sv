module SimEbreak (
    input valid,
    input [31:0] is_ebreak // 仅仅为了防止优化，或者传递标记
);
    import "DPI-C" function void ebreak(); 
    
    always @(*) begin
        if (valid) begin
            ebreak();
        end
    end
endmodule