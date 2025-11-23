module SimState (
    input logic          clk,
    input logic          reset,
    input logic [31:0]   pc,
    input logic [1023:0] regs_flat,
    // [新增]
    input logic [31:0]   mtvec,
    input logic [31:0]   mepc,
    input logic [31:0]   mstatus,
    input logic [31:0]   mcause
);
    // 更新 DPI 函数签名
    import "DPI-C" function void dpi_update_state(
        input int pc, 
        input bit [1023:0] gprs,
        input int mtvec,
        input int mepc,
        input int mstatus,
        input int mcause
    );

    always @(posedge clk) begin
        if (!reset) begin
            dpi_update_state(pc, regs_flat, mtvec, mepc, mstatus, mcause);
        end
    end
endmodule