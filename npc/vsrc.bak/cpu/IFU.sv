// IFU.sv
// Moore FSM 实现, PC更新由WBU驱动, 包含首次启动逻辑。
import cpu_types_pkg::*;

module IFU (
    input  logic        clk, rst,
    pc_redirect_if.slave pc_redirect_port,
    AXI4_Lite.master ifu_axi_if,


    //只用读口：
    //先进入busy状态之后，发送地址，以及valid， 等待ready，ready之后拉低valid， 进入下一个状态，
    //同时我们不必担忧这个时钟的延时会导致valid延迟了一个时钟拉低导致mem再次被触发
    //mem会等待cpu接收到rdata
    //在下一个状态，我们等待mem发出valid
    stage_if.master     if_out 
);
    // 状态定义
    typedef enum logic[1:0] {
        S_IDLE,       // 等待启动信号
        S_SEND_ADDR,  // 发送读地址
        S_WAIT_DATA,  // 等待内存返回数据
        S_WAIT_IDU    // 等待下游IDU接收
    } state_e;
    //我们需要增加状态：
    //IDLE受到请求之后进入busy，同时发出请求信号，在busy状态等待arready，若fire，进入下一个状态，等待rready
    
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
            if (next_state == S_SEND_ADDR && cur_state == S_IDLE) begin
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
            blank <= 1'b0;
        end else begin
            // PC只在即将开始一次新的取指时更新 (即, 从IDLE进入BUSY)
            // PC在IDLE->SEND_ADDR时更新
            if (cur_state == S_IDLE && next_state == S_SEND_ADDR) begin
                if (cpu_started) begin
                    pc <= pc_redirect_port.target;
                end
                // 首次启动时PC保持复位值
            end

            // 当指令成功取回时，锁存指令和对应的PC
            if (ifu_axi_if.r_fire) begin
                inst_reg <= ifu_axi_if.rdata;
                pc_reg   <= ifu_axi_if.araddr; // araddr在地址发送后是稳定的
                blank <= 1'b1;
            end
        end
    end

    // 状态转移逻辑 (组合逻辑)
    always_comb begin
        next_state = cur_state;
        unique case (cur_state)
            S_IDLE: begin
                if (!cpu_started || pc_redirect_port.valid) begin
                    next_state = S_SEND_ADDR;
                    //$display("begin send addr");
                end
            end
            S_SEND_ADDR: begin
                // 地址成功发送后，进入等待数据状态
                if (ifu_axi_if.ar_fire) begin
                    next_state = S_WAIT_DATA;
                    //$display("begin wait data");
                end
            end
            S_WAIT_DATA: begin
                // 数据成功接收后，进入等待下游状态
                if (ifu_axi_if.r_fire) begin
                    next_state = S_WAIT_IDU;
                    //$display("begin wait idu");
                end
            end
            S_WAIT_IDU: begin
                if (if_out.ready) begin
                    next_state = S_IDLE;
                end
            end
        endcase
    end

        // 输出逻辑 (组合逻辑)
    always_comb begin
        // --- AXI Master Interface ---
        ifu_axi_if.araddr  = pc;
        ifu_axi_if.arvalid = 1'b0;
        ifu_axi_if.rready  = 1'b0;
        // (IFU不写内存, 其余AXI信号保持默认低电平或不驱动)
        ifu_axi_if.awaddr = 'x; ifu_axi_if.awvalid = 1'b0;
        ifu_axi_if.wdata = 'x; ifu_axi_if.wstrb = 'x; ifu_axi_if.wvalid = 1'b0;
        ifu_axi_if.bready = 1'b0;
        
        // --- Pipeline Interface ---
        if_out.valid = 1'b0;
        if_out.payload.inst = inst_reg;
        if_out.payload.pc = pc_reg;
        if_out.payload.valid = 1'b0;

        unique case (cur_state)
            S_SEND_ADDR: begin
                ifu_axi_if.arvalid = 1'b1;
            end
            S_WAIT_DATA: begin
                ifu_axi_if.rready = 1'b1;
            end
            S_WAIT_IDU: begin
                if_out.valid = 1'b1;
                if_out.payload.valid = 1'b1;
            end
            default:; // S_IDLE
        endcase
    end
endmodule