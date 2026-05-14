module NpcVirtualAxiRam(
  input  logic        clock,
  input  logic        reset,

  input  logic        aw_valid,
  output logic        aw_ready,
  input  logic [31:0] aw_addr,
  input  logic [3:0]  aw_id,

  input  logic        w_valid,
  output logic        w_ready,
  input  logic [31:0] w_data,
  input  logic [3:0]  w_strb,

  output logic        b_valid,
  input  logic        b_ready,
  output logic [1:0]  b_resp,
  output logic [3:0]  b_id,

  input  logic        ar_valid,
  output logic        ar_ready,
  input  logic [31:0] ar_addr,
  input  logic [3:0]  ar_id,

  output logic        r_valid,
  input  logic        r_ready,
  output logic [1:0]  r_resp,
  output logic [31:0] r_data,
  output logic        r_last,
  output logic [3:0]  r_id
);
  import "DPI-C" function int  npc_pmem_read(input int raddr);
  import "DPI-C" function void npc_pmem_write(input int waddr, input int wdata, input byte wmask);

  logic aw_seen;
  logic [31:0] aw_addr_reg;
  logic [3:0]  aw_id_reg;
  logic w_seen;
  logic [31:0] w_data_reg;
  logic [3:0]  w_strb_reg;

  wire aw_fire = aw_valid && aw_ready;
  wire w_fire = w_valid && w_ready;
  wire ar_fire = ar_valid && ar_ready;

  assign aw_ready = !reset && !b_valid && !aw_seen;
  assign w_ready = !reset && !b_valid && !w_seen;
  assign ar_ready = !reset && !r_valid;
  assign b_resp = 2'b00;
  assign r_resp = 2'b00;
  assign r_last = 1'b1;

  always_ff @(posedge clock) begin
    if (reset) begin
      r_valid <= 1'b0;
      r_data <= 32'b0;
      r_id <= 4'b0;
      b_valid <= 1'b0;
      b_id <= 4'b0;
      aw_seen <= 1'b0;
      aw_addr_reg <= 32'b0;
      aw_id_reg <= 4'b0;
      w_seen <= 1'b0;
      w_data_reg <= 32'b0;
      w_strb_reg <= 4'b0;
    end else begin
      if (r_valid && r_ready) begin
        r_valid <= 1'b0;
      end

      if (b_valid && b_ready) begin
        b_valid <= 1'b0;
      end

      if (ar_fire) begin
        r_valid <= 1'b1;
        r_data <= npc_pmem_read(ar_addr);
        r_id <= ar_id;
      end

      if (!b_valid) begin
        if (aw_fire && !(aw_seen || w_seen || w_fire)) begin
          aw_seen <= 1'b1;
          aw_addr_reg <= aw_addr;
          aw_id_reg <= aw_id;
        end
        if (w_fire && !(w_seen || aw_seen || aw_fire)) begin
          w_seen <= 1'b1;
          w_data_reg <= w_data;
          w_strb_reg <= w_strb;
        end

        if ((aw_seen || aw_fire) && (w_seen || w_fire)) begin
          npc_pmem_write(aw_fire ? aw_addr : aw_addr_reg,
                         w_fire ? w_data : w_data_reg,
                         w_fire ? {4'b0, w_strb} : {4'b0, w_strb_reg});
          b_valid <= 1'b1;
          b_id <= aw_fire ? aw_id : aw_id_reg;
          aw_seen <= 1'b0;
          w_seen <= 1'b0;
        end
      end
    end
  end
endmodule
