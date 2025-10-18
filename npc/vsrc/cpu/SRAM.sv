
// Memory.v
// A unified memory module for both instruction fetch and data access.
// This module is purely combinational and uses DPI-C.

module SRAM (
    // Instruction Fetch Port
    input logic clk,
    input logic rst,

    AXI4_Lite.slave sram_axi_if
    //在得到valid 之后，跳转busy，并发出pmem_read请求（可以使用时序块），于是在下一个周期之后，得到了数据
    //然后等待发出rvalid，等待cpu。在cpu_ready之后，拉低
    //读的状态：IDLE, WAIT_CPU， 对于ready，在IDLE情况下永远ready


    //对于写：IDLE情况下，可以让data和addr同时ready，同时master不依赖于这个ready
    //接收到地址valid：如果数据也是valid，那么直接write，并跳转到等待cpu，
    //如果没有valid，可以等待cpudata
);

    // Import the C functions that will perform the actual memory operations.
    import "DPI-C" function int pmem_read(input int raddr);
    import "DPI-C" function void pmem_write(input int waddr, input int wdata, input byte wmask);

    typedef enum logic [1:0] {
        S_IDLE, S_R_WAIT_CPU, S_W_WAIT_CPU, S_B_WAIT_CPU
    } state_e;

    state_e cur_state, next_state;

    logic [31:0] rdata_reg;
    logic [31:0] awaddr_reg;
    always_ff @(posedge clk) begin
        if  (rst) begin
            cur_state <= S_IDLE;
            rdata_reg <= 0;
            awaddr_reg <= 0;
        end else 
            cur_state <= next_state;
    end

    always_ff @(posedge clk) begin
        if (cur_state == S_IDLE && next_state == S_R_WAIT_CPU) begin
            rdata_reg <= pmem_read(sram_axi_if.araddr);
        end 
        if (cur_state == S_IDLE && next_state == S_B_WAIT_CPU) begin
            pmem_write(sram_axi_if.awaddr, sram_axi_if.wdata, sram_axi_if.wstrb);
        end
        if (cur_state == S_IDLE && next_state == S_W_WAIT_CPU) begin
            awaddr_reg <= sram_axi_if.awaddr;
        end
        if (cur_state == S_W_WAIT_CPU && next_state == S_B_WAIT_CPU) begin
            pmem_write(awaddr_reg, sram_axi_if.wdata, sram_axi_if.wstrb);
        end
    end

    always_comb begin
        next_state = cur_state;
        case (cur_state) 
            S_IDLE: begin
                if (sram_axi_if.ar_fire) begin
                    next_state = S_R_WAIT_CPU;
                    //$display("begin R wait cpu");
                end
                else if (sram_axi_if.aw_fire) begin
                    if (sram_axi_if.w_fire)
                        next_state = S_B_WAIT_CPU;
                    else
                        next_state = S_W_WAIT_CPU;
                end
            end
                

            S_R_WAIT_CPU: begin
                if (sram_axi_if.r_fire) begin
                    next_state = S_IDLE;
                    //$display("return rdata");
                end
            end


            S_W_WAIT_CPU:
                if (sram_axi_if.w_fire)
                    next_state = S_B_WAIT_CPU;
            
            S_B_WAIT_CPU:
                if (sram_axi_if.b_fire)
                    next_state = S_IDLE;

            default: begin end
        endcase
            
    end

    always_comb begin
        sram_axi_if.awready = 1'b0;
        sram_axi_if.wready  = 1'b0;
        sram_axi_if.bvalid  = 1'b0;
        sram_axi_if.bresp   = 2'b00; // OKAY

        sram_axi_if.arready = 1'b0;
        sram_axi_if.rvalid  = 1'b0;
        sram_axi_if.rdata   = rdata_reg;
        sram_axi_if.rresp   = 2'b00; // OKAY
        unique case (cur_state)
            S_IDLE: begin
                sram_axi_if.arready = 1'b1;
                sram_axi_if.awready = 1'b1;
                sram_axi_if.wready = 1'b1;
            end
            S_R_WAIT_CPU: begin
                sram_axi_if.rvalid = 1'b1;
            end
            S_W_WAIT_CPU: begin
                sram_axi_if.wready = 1'b1;
            end
            S_B_WAIT_CPU: begin
                sram_axi_if.bvalid = 1'b1;
            end

        endcase
    end



    


endmodule
