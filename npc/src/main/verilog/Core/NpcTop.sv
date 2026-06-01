module NpcTop(
  input logic clock,
  input logic reset
);
  wire        master_awready;
  wire        master_awvalid;
  wire [31:0] master_awaddr;
  wire [3:0]  master_awid;
  wire [7:0]  master_awlen;
  wire [2:0]  master_awsize;
  wire [1:0]  master_awburst;
  wire        master_wready;
  wire        master_wvalid;
  wire [31:0] master_wdata;
  wire [3:0]  master_wstrb;
  wire        master_wlast;
  wire        master_bready;
  wire        master_bvalid;
  wire [1:0]  master_bresp;
  wire [3:0]  master_bid;
  wire        master_arready;
  wire        master_arvalid;
  wire [31:0] master_araddr;
  wire [3:0]  master_arid;
  wire [7:0]  master_arlen;
  wire [2:0]  master_arsize;
  wire [1:0]  master_arburst;
  wire        master_rready;
  wire        master_rvalid;
  wire [1:0]  master_rresp;
  wire [31:0] master_rdata;
  wire        master_rlast;
  wire [3:0]  master_rid;

  myCore core (
    .clock(clock),
    .reset(reset),
    .io_interrupt(1'b0),
    .io_master_awready(master_awready),
    .io_master_awvalid(master_awvalid),
    .io_master_awaddr(master_awaddr),
    .io_master_awid(master_awid),
    .io_master_awlen(master_awlen),
    .io_master_awsize(master_awsize),
    .io_master_awburst(master_awburst),
    .io_master_wready(master_wready),
    .io_master_wvalid(master_wvalid),
    .io_master_wdata(master_wdata),
    .io_master_wstrb(master_wstrb),
    .io_master_wlast(master_wlast),
    .io_master_bready(master_bready),
    .io_master_bvalid(master_bvalid),
    .io_master_bresp(master_bresp),
    .io_master_bid(master_bid),
    .io_master_arready(master_arready),
    .io_master_arvalid(master_arvalid),
    .io_master_araddr(master_araddr),
    .io_master_arid(master_arid),
    .io_master_arlen(master_arlen),
    .io_master_arsize(master_arsize),
    .io_master_arburst(master_arburst),
    .io_master_rready(master_rready),
    .io_master_rvalid(master_rvalid),
    .io_master_rresp(master_rresp),
    .io_master_rdata(master_rdata),
    .io_master_rlast(master_rlast),
    .io_master_rid(master_rid),
    .io_slave_awready(),
    .io_slave_awvalid(1'b0),
    .io_slave_awaddr(32'b0),
    .io_slave_awid(4'b0),
    .io_slave_awlen(8'b0),
    .io_slave_awsize(3'b0),
    .io_slave_awburst(2'b0),
    .io_slave_wready(),
    .io_slave_wvalid(1'b0),
    .io_slave_wdata(32'b0),
    .io_slave_wstrb(4'b0),
    .io_slave_wlast(1'b0),
    .io_slave_bready(1'b0),
    .io_slave_bvalid(),
    .io_slave_bresp(),
    .io_slave_bid(),
    .io_slave_arready(),
    .io_slave_arvalid(1'b0),
    .io_slave_araddr(32'b0),
    .io_slave_arid(4'b0),
    .io_slave_arlen(8'b0),
    .io_slave_arsize(3'b0),
    .io_slave_arburst(2'b0),
    .io_slave_rready(1'b0),
    .io_slave_rvalid(),
    .io_slave_rresp(),
    .io_slave_rdata(),
    .io_slave_rlast(),
    .io_slave_rid()
  );

  NpcVirtualAxiRam ram (
    .clock(clock),
    .reset(reset),
    .io_aw_valid(master_awvalid),
    .io_aw_ready(master_awready),
    .io_aw_addr(master_awaddr),
    .io_aw_id(master_awid),
    .io_aw_len(master_awlen),
    .io_aw_size(master_awsize),
    .io_aw_burst(master_awburst),
    .io_w_valid(master_wvalid),
    .io_w_ready(master_wready),
    .io_w_data(master_wdata),
    .io_w_strb(master_wstrb),
    .io_w_last(master_wlast),
    .io_b_valid(master_bvalid),
    .io_b_ready(master_bready),
    .io_b_resp(master_bresp),
    .io_b_id(master_bid),
    .io_ar_valid(master_arvalid),
    .io_ar_ready(master_arready),
    .io_ar_addr(master_araddr),
    .io_ar_id(master_arid),
    .io_ar_len(master_arlen),
    .io_ar_size(master_arsize),
    .io_ar_burst(master_arburst),
    .io_r_valid(master_rvalid),
    .io_r_ready(master_rready),
    .io_r_resp(master_rresp),
    .io_r_data(master_rdata),
    .io_r_last(master_rlast),
    .io_r_id(master_rid)
  );
endmodule
