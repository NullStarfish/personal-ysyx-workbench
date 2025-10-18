// top.sv
// 顶层模块，用于实例化并连接CPU流水线的所有组件。


import cpu_types_pkg::*; // 导入在 cpu_types_pkg.sv 中定义的各种数据结构

module Top (
    input logic clk, // 时钟信号
    input logic rst  // 复位信号
);

    //----------------------------------------------------------------
    // 流水线级间接口 (Pipeline Stage Interfaces)
    //----------------------------------------------------------------
    stage_if #(if_id_t)  if_id_if();  // IFU -> IDU
    stage_if #(id_ex_t)  id_ex_if();  // IDU -> EXU
    stage_if #(ex_lsu_t) ex_lsu_if(); // EXU -> LSU
    stage_if #(lsu_wb_t) lsu_wb_if(); // LSU -> WBU




    AXI4_Lite ifu_sram();
    AXI4_Lite lsu_sram();

    //----------------------------------------------------------------
    // 反馈路径接口 (Feedback Interfaces)
    //----------------------------------------------------------------
    // 从 WBU 到 IDU 的寄存器写回接口
    regfile_write_if reg_write_if();
    // 从 WBU 到 IFU 的 PC 重定向接口 (用于分支和跳转)
    pc_redirect_if   pc_redirect_if();

    //----------------------------------------------------------------
    // 连接到内存模块的信号 (Memory Signals)
    //----------------------------------------------------------------
    // 指令内存端口 (Instruction Port)
    logic [31:0] i_addr;
    logic        i_addr_valid;
    logic [31:0] i_rdata;
    logic        i_rdata_valid;

    // 数据内存端口 (Data Port)
    logic [31:0] d_addr;
    logic [31:0] d_wdata;
    logic [7:0]  d_wmask;
    logic        d_wen;
    logic [31:0] d_rdata_raw;
    logic        d_ready;
    logic        d_valid;


    //----------------------------------------------------------------
    // 流水线模块实例化 (Pipeline Modules Instantiation)
    //----------------------------------------------------------------

    // 阶段 1: 取指单元 (Instruction Fetch Unit)
    IFU u_ifu (
        .clk              (clk),
        .rst              (rst),
        .pc_redirect_port (pc_redirect_if.slave), // 来自 WBU 的 PC 重定向请求
        .ifu_axi_if       (ifu_sram),
        .if_out           (if_id_if.master)         // 输出到 IDU
    );

    // 阶段 2: 译码单元 (Instruction Decode Unit)
    IDU u_idu (
        .clk            (clk),
        .rst            (rst),
        .reg_write_port (reg_write_if.slave), // 来自 WBU 的寄存器写回数据
        .id_in          (if_id_if.slave),     // 来自 IFU 的输入
        .id_out         (id_ex_if.master)     // 输出到 EXU
    );

    // 阶段 3: 执行单元 (Execute Unit)
    EXU u_exu (
        .clk    (clk),
        .rst    (rst),
        .ex_in  (id_ex_if.slave),     // 来自 IDU 的输入
        .ex_out (ex_lsu_if.master)    // 输出到 LSU
    );

    // 阶段 4: 访存单元 (Load/Store Unit)
    LSU u_lsu (
        .clk         (clk),
        .rst         (rst),
        .lsu_axi_if  (lsu_sram),
        .lsu_in      (ex_lsu_if.slave),  // 来自 EXU 的输入
        .lsu_out     (lsu_wb_if.master)  // 输出到 WBU
    );

    // 阶段 5: 写回单元 (Write-Back Unit)
    WBU u_wbu (
        .clk              (clk),
        .rst              (rst),
        .reg_write_port   (reg_write_if.master),   // 输出到 IDU 的寄存器写回端口
        .pc_redirect_port (pc_redirect_if.master), // 输出到 IFU 的 PC 重定向端口
        .wb_in            (lsu_wb_if.slave)        // 来自 LSU 的输入
    );

    //----------------------------------------------------------------
    // 内存模块实例化 (Memory Instantiation)
    //----------------------------------------------------------------
    AXI4_Lite to_sram();
    AXI_Pipelined_Bridge_Arbiter u_arbiter (
        .clk              (clk),
        .rst              (rst),
        .slave0_if        (ifu_sram),
        .slave1_if        (lsu_sram),
        .master_if        (to_sram)
    );

    SRAM u_sram (
        .clk              (clk),
        .rst              (rst),
        .sram_axi_if      (to_sram)
    );


endmodule