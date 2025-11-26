module SimState (
    input logic          clk,
    input logic          reset,
    input logic          valid, // [新增]
    input logic [31:0]   pc,
    input logic [31:0]   dnpc, // [新增]
    input logic [1023:0] regs_flat,
    input logic [31:0]   mtvec,
    input logic [31:0]   mepc,
    input logic [31:0]   mstatus,
    input logic [31:0]   mcause,
    input logic [31:0]   inst
);
    import "DPI-C" function void dpi_update_state(
        input int pc, 
        input int dnpc, // [新增]
        input bit [1023:0] gprs,
        input int mtvec,
        input int mepc,
        input int mstatus,
        input int mcause,
        input int inst
    );



    reg start;
    always @(posedge clk) begin
        // [修改] 只有 valid=1 时才向 C++ 环境推送状态
        if (!reset && valid) begin
            start <= 1;
        end else
            start <= 0;


        if (start == 1)
            dpi_update_state(pc, dnpc, regs_flat, mtvec, mepc, mstatus, mcause, inst);
    end

    
endmodule