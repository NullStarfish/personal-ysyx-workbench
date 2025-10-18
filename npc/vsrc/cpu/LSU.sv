// LSU.sv
// (已修改为使用 AXI4-Lite Master 接口)
import cpu_types_pkg::*;

module LSU (
    input  logic       clk, rst,

    // --- [MODIFIED] Memory Interface: Changed to AXI4-Lite Master ---
    AXI4_Lite.master   lsu_axi_if,

    // --- Pipeline Interface ---
    stage_if.slave     lsu_in,
    stage_if.master    lsu_out
);

    // --- 内部信号和解码器 ---
    logic [31:0] load_out;
    logic [31:0] store_wdata;
    logic [3:0]  store_wmask; // AXI wstrb for 32-bit data is 4 bits wide

    // LOAD解码器的输入数据源现在是AXI读数据通道
    LOAD_Decoder u_LOAD_Decoder(
        .raw_addr 	(ex_lsu_payload_reg.mem_addr),
        .raw_data 	(lsu_axi_if.rdata), // [MODIFIED] 从AXI接口获取读数据
        .funct3   	(ex_lsu_payload_reg.funct3),
        .out      	(load_out)
    );

    // STORE解码器的输出现在连接到内部信号，稍后驱动AXI写数据通道
    STORE_Decoder u_STORE_Decoder(
        .raw_addr 	(ex_lsu_payload_reg.mem_addr),
        .raw_data 	(ex_lsu_payload_reg.mem_wdata),
        .funct3   	(ex_lsu_payload_reg.funct3),
        .wmask    	(store_wmask), // [MODIFIED] 输出到内部信号
        .wdata    	(store_wdata)  // [MODIFIED] 输出到内部信号
    );

    // --- 流水线寄存器 ---
    ex_lsu_t ex_lsu_payload_reg;
    lsu_wb_t lsu_wb_payload_reg;

    wire lsu_in_fire = lsu_in.valid && lsu_in.ready;

    // 从EXU接收数据的输入寄存器逻辑保持不变
    always_ff @(posedge clk) begin
        if (rst) begin
            ex_lsu_payload_reg.valid <= 1'b0;
        end else if (lsu_in_fire) begin
            ex_lsu_payload_reg <= lsu_in.payload;
        end else if (cur_state == S_WAIT_WBU && lsu_out.ready) begin
            // 当指令被下游接收后，使其无效，防止重复执行
            ex_lsu_payload_reg.valid <= 1'b0;
        end
    end

    // --- [REVISED] FSM for AXI4-Lite Handshake Logic ---
    // 状态机需要扩展以处理AXI的读、写和响应通道
    typedef enum logic [2:0] {
        S_IDLE,             // 等待来自EXU的指令
        S_PROCESSING,       // 分析指令，决定是内存操作还是直通
        S_READ_ADDR,        // 发送读地址
        S_READ_DATA,        // 等待读数据返回
        S_WRITE_ADDR_DATA,  // 同时发送写地址和写数据
        S_WRITE_RESP,       // 等待写响应
        S_WAIT_WBU          // 操作完成，等待WBU接收
    } lsu_state_e;
    
    lsu_state_e cur_state, next_state;

    always_ff @(posedge clk) begin
        if (rst) begin
            cur_state <= S_IDLE;
        end else begin
            cur_state <= next_state;
        end
    end

    // FSM 状态转移逻辑
    always_comb begin
        next_state = cur_state;
        unique case (cur_state)
            S_IDLE: begin
                if (lsu_in.valid) begin
                    next_state = S_PROCESSING;
                end
            end
            S_PROCESSING: begin
                // 根据指令类型决定下一步
                if (ex_lsu_payload_reg.mem_en) begin
                    if (ex_lsu_payload_reg.mem_wen) // Store operation
                        next_state = S_WRITE_ADDR_DATA;
                    else // Load operation
                        next_state = S_READ_ADDR;
                end else begin // Pass-through (e.g., ALU op)
                    next_state = S_WAIT_WBU;
                end
            end
            S_READ_ADDR: begin // 发送读地址状态
                if (lsu_axi_if.ar_fire) begin
                    next_state = S_READ_DATA;
                end
            end
            S_READ_DATA: begin // 等待读数据状态
                if (lsu_axi_if.r_fire) begin
                    next_state = S_WAIT_WBU;
                end
            end
            S_WRITE_ADDR_DATA: begin // 发送写地址和数据状态
                // AXI4-Lite允许地址和数据通道独立握手
                // 当两者都成功发送后，进入等待响应状态
                if (lsu_axi_if.aw_fire && lsu_axi_if.w_fire) begin
                    next_state = S_WRITE_RESP;
                end
            end
            S_WRITE_RESP: begin // 等待写响应状态
                if (lsu_axi_if.b_fire) begin
                    next_state = S_WAIT_WBU;
                end
            end
            S_WAIT_WBU: begin // 等待下游接收状态
                if (lsu_out.ready) begin
                    next_state = S_IDLE;
                end
            end
        endcase
    end
    
    // --- 数据路径和输出逻辑 ---
    
    // 准备发送到WBU的数据
    lsu_wb_t lsu_wb_payload_next;
    always_comb begin
        lsu_wb_payload_next.valid     = ex_lsu_payload_reg.valid;
        lsu_wb_payload_next.reg_wen   = ex_lsu_payload_reg.reg_wen;
        lsu_wb_payload_next.rd_addr   = ex_lsu_payload_reg.rd_addr;
        lsu_wb_payload_next.pc_target = ex_lsu_payload_reg.pc_target;
        
        // 如果是Load指令，写回数据来自内存；否则来自EXU的结果
        if (ex_lsu_payload_reg.mem_en && !ex_lsu_payload_reg.mem_wen) begin
             lsu_wb_payload_next.wb_data = load_out;
        end else begin
             lsu_wb_payload_next.wb_data = ex_lsu_payload_reg.exu_result;
        end
    end

    // 当操作完成时，锁存最终结果到输出寄存器
    always_ff @(posedge clk) begin
        if (rst) begin
            lsu_wb_payload_reg.valid <= 1'b0;
        end else if ( (cur_state == S_PROCESSING && !ex_lsu_payload_reg.mem_en) || // Non-mem op completes
                      (cur_state == S_READ_DATA && lsu_axi_if.r_fire)           || // Read op completes
                      (cur_state == S_WRITE_RESP && lsu_axi_if.b_fire) ) begin      // Write op completes
            lsu_wb_payload_reg <= lsu_wb_payload_next;
        end
    end

    // 控制流水线握手和AXI接口信号
    always_comb begin
        // --- 默认值 ---
        lsu_in.ready = 1'b0;
        lsu_out.valid = 1'b0;
        lsu_out.payload = lsu_wb_payload_reg;
        
        // AXI Master Interface - 默认不发起任何请求
        lsu_axi_if.araddr = 'x; lsu_axi_if.arvalid = 1'b0;
        lsu_axi_if.rready = 1'b0;
        lsu_axi_if.awaddr = 'x; lsu_axi_if.awvalid = 1'b0;
        lsu_axi_if.wdata  = 'x; lsu_axi_if.wstrb  = 'x; lsu_axi_if.wvalid = 1'b0;
        lsu_axi_if.bready = 1'b0;
        
        unique case (cur_state)
            S_IDLE: begin
                lsu_in.ready = 1'b1;
            end
            S_READ_ADDR: begin
                lsu_axi_if.arvalid = 1'b1;
                lsu_axi_if.araddr  = ex_lsu_payload_reg.mem_addr;
            end
            S_READ_DATA: begin
                lsu_axi_if.rready = 1'b1;
            end
            S_WRITE_ADDR_DATA: begin
                // 同时驱动写地址和写数据通道
                lsu_axi_if.awvalid = 1'b1;
                lsu_axi_if.awaddr  = ex_lsu_payload_reg.mem_addr;
                lsu_axi_if.wvalid  = 1'b1;
                lsu_axi_if.wdata   = store_wdata;
                lsu_axi_if.wstrb   = store_wmask;
            end
            S_WRITE_RESP: begin
                lsu_axi_if.bready = 1'b1;
            end
            S_WAIT_WBU: begin
                lsu_out.valid = 1'b1;
            end
            default:; // S_PROCESSING
        endcase
    end
endmodule