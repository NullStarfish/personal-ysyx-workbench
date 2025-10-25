// interfaces.sv
// Defines all handshake and feedback interfaces used in the CPU.

// Generic stage-to-stage handshake interface
interface stage_if #(parameter type PAYLOAD = logic [31:0]);
    PAYLOAD      payload;
    logic        valid;
    logic        ready;
    wire         fire = valid && ready;
    modport master (output payload, output valid, input ready, input fire);
    modport slave  (input payload, input valid, output ready, input fire);

    wire fire = valid && ready;
endinterface


// Feedback interface for register file writes
interface regfile_write_if;
    logic        wen;
    logic [4:0]  addr;
    logic [31:0] data;

    modport master (output wen, addr, data);
    modport slave  (input wen, addr, data);
endinterface

// Feedback interface for PC redirection (branches/jumps)
interface pc_redirect_if;
    logic        valid;
    logic [31:0] target;

    modport master (output valid, target);
    modport slave  (input valid, target);
endinterface

interface AXI4_Lite;
    // --- 信号定义 ---
    // 总线宽度可以根据需要进行参数化
    parameter int ADDR_WIDTH = 32;
    parameter int DATA_WIDTH = 32;
    parameter int STRB_WIDTH = DATA_WIDTH / 8;

    // 写地址通道 (Write Address Channel)
    logic [ADDR_WIDTH-1:0] awaddr;
    logic                  awvalid;
    logic                  awready;

    // 写数据通道 (Write Data Channel)
    logic [DATA_WIDTH-1:0] wdata;
    logic [STRB_WIDTH-1:0] wstrb;
    logic                  wvalid;
    logic                  wready;

    // 写响应通道 (Write Response Channel)
    logic [1:0]            bresp;
    logic                  bvalid;
    logic                  bready;

    // 读地址通道 (Read Address Channel)
    logic [ADDR_WIDTH-1:0] araddr;
    logic                  arvalid;
    logic                  arready;

    // 读数据通道 (Read Data Channel)
    logic [DATA_WIDTH-1:0] rdata;
    logic [1:0]            rresp;
    logic                  rvalid;
    logic                  rready;

    // --- Fire 信号 ---
    // 当 VALID 和 READY 同时为高时，表示握手成功，数据成功传输
    logic aw_fire;
    logic w_fire;
    logic b_fire;
    logic ar_fire;
    logic r_fire;

    assign aw_fire = awvalid && awready;
    assign w_fire  = wvalid  && wready;
    assign b_fire  = bvalid  && bready;
    assign ar_fire = arvalid && arready;
    assign r_fire  = rvalid  && rready;


    // --- Master Modport ---
    // Master驱动地址、数据和控制信号，接收响应和就绪信号
    modport master (
        // Read Address Channel
        output araddr,
        output arvalid,
        input  arready,

        // Read Data Channel
        input  rdata,
        input  rresp,
        input  rvalid,
        output rready,

        // Write Address Channel
        output awaddr,
        output awvalid,
        input  awready,

        // Write Data Channel
        output wdata,
        output wstrb,
        output wvalid,
        input  wready,

        // Write Response Channel
        input  bresp,
        input  bvalid,
        output bready,

        input aw_fire, w_fire, ar_fire, r_fire, b_fire

    );

    // --- Slave Modport ---
    // Slave接收地址、数据和控制信号，驱动响应和就绪信号
    modport slave (
        // Read Address Channel
        input  araddr,
        input  arvalid,
        output arready,

        // Read Data Channel
        output rdata,
        output rresp,
        output rvalid,
        input  rready,

        // Write Address Channel
        input  awaddr,
        input  awvalid,
        output awready,

        // Write Data Channel
        input  wdata,
        input  wstrb,
        input  wvalid,
        output wready,

        // Write Response Channel
        output bresp,
        output bvalid,
        input  bready,



        input aw_fire, w_fire, ar_fire, r_fire, b_fire
    );
endinterface