// IFU.sv
// Moore FSM 实现, PC更新由WBU驱动, 包含首次启动逻辑。
import cpu_types_pkg::*;

module IFU (
    input  logic        clk, rst,
    pc_redirect_if.slave pc_redirect_port,
    output logic [31:0] i_addr,
    output logic        i_addr_valid,
    input  logic        i_rdata_valid, 
    input  logic [31:0] i_rdata,
    stage_if.master     if_out 
);
    // 状态定义
    typedef enum logic[1:0] {
        S_IDLE,       // 空闲状态, 等待WBU指令启动下一次取指
        S_BUSY,       // 已向内存发出请求, 等待内存返回数据
        S_WAIT_READY  // 已收到数据, 等待下游IDU接收
    } state_e;
    
    state_e cur_state, next_state;

    // 数据路径寄存器
    logic [31:0] pc /* verilator public */ ;       // Program Counter
    logic [31:0] inst_reg /* verilator public */; // 锁存从内存读出的指令
    logic [31:0] pc_reg   /* verilator public */; // 锁存指令对应的PC

    logic [31:0] i_addr_reg; 

    // 控制逻辑寄存器
    logic        cpu_started; // 标志位, 用于处理CPU的首次启动取指

    // 状态和控制寄存器 (时序逻辑)
    always_ff @(posedge clk) begin
        if (rst) begin
            cur_state <= S_IDLE;
            cpu_started <= 1'b0; // 复位时, CPU未启动
        end else begin
            cur_state <= next_state;
            // 当状态机决定开始一次取指时, 将启动位置为1
            if (next_state == S_BUSY && cur_state == S_IDLE) begin
                cpu_started <= 1'b1;
            end
        end
    end

    // PC 和数据锁存 (时序逻辑)
    logic blank /* verilator public */ ;
    always_ff @(posedge clk) begin
        if (rst) begin
            pc       <= 32'h80000000; // 设置复位向量
            inst_reg <= 32'b0;
            pc_reg   <= 32'h80000000;
            i_addr_reg <= 32'h80000000;
            blank <= 1'b0;
        end else begin
            // PC只在即将开始一次新的取指时更新 (即, 从IDLE进入BUSY)
            if (next_state == S_BUSY && cur_state == S_IDLE) begin
                // 如果是正常运行 (非首次启动), PC值来自WBU
                if (cpu_started) begin
                    pc <= pc_redirect_port.target;
                end
                // 如果是首次启动, PC保持复位向量, 无需更新
            end

                // [NEW] 当我们发出内存请求时，锁存当前的PC值
            if (next_state == S_BUSY && cur_state == S_IDLE) begin
                if (cpu_started) begin
                    i_addr_reg <= pc_redirect_port.target;
                end else begin
                    i_addr_reg <= pc; // For the very first fetch
                end
            end

            // [MODIFIED] 当内存返回数据时...
            if (cur_state == S_BUSY && i_rdata_valid) begin
                // 使用之前锁存的地址来显示和更新pc_reg
                //$display("IFU: Fetched instruction 0x%08x from address 0x%08x", i_rdata, i_addr_reg);
                inst_reg <= i_rdata;
                pc_reg   <= i_addr_reg; // <-- 关键修改！使用延迟的PC
                blank <= 1'b1;
            end
        end
    end

    // 状态转移逻辑 (组合逻辑)
    always_comb begin
        next_state = cur_state; // 默认保持当前状态
        unique case (cur_state)
            S_IDLE: begin
                // 如果是首次启动, 则无条件开始取指。
                // 否则, 必须等待WBU的有效重定向信号。
                if (!cpu_started || pc_redirect_port.valid) begin
                    //$display("IFU: Starting instruction fetch at PC = 0x%08x", pc);
                    next_state = S_BUSY;
                end
            end
            S_BUSY: begin
                // 在BUSY状态, 如果收到了内存的有效数据, 则进入等待下游状态
                if (i_rdata_valid) begin
                    next_state = S_WAIT_READY;
                end
            end
            S_WAIT_READY: begin
                // 在WAIT_READY状态, 如果下游接收了数据, 则返回IDLE等待下一次启动信号
                if (if_out.ready) begin
                    next_state = S_IDLE;
                end
            end
        endcase
    end

    // 输出逻辑 (组合逻辑, Moore 类型, 只依赖于当前状态)
    always_comb begin
        // --- 默认输出 ---
        i_addr = pc;
        i_addr_valid = 1'b0;
        if_out.valid = 1'b0;
        if_out.payload.inst = inst_reg; // 总是输出寄存器中的值
        if_out.payload.pc = pc_reg;     // 总是输出寄存器中的值
        if_out.payload.valid = 1'b0;    // 默认无效

        unique case (cur_state)
            S_BUSY: begin
                // BUSY状态: 向内存发出有效请求
                i_addr_valid = 1'b1;
            end
            S_WAIT_READY: begin
                // WAIT_READY状态: 向下游提供有效数据
                
                if_out.valid = 1'b1;
                if_out.payload.valid = 1'b1;
            end
            default: begin
                // S_IDLE状态: 无任何有效输出, 使用默认值即可
            end
            // S_IDLE状态: 无任何有效输出, 使用默认值即可
        endcase
    end

endmodule