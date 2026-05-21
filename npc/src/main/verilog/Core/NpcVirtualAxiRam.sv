module NpcVirtualAxiRam #(
  parameter int unsigned READ_LATENCY = 100,
  parameter int unsigned WRITE_LATENCY = 100,
  parameter int unsigned READ_DEPTH = 16,
  parameter int unsigned WRITE_DEPTH = 16
) (
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

  localparam int unsigned READ_CNT_W = (READ_DEPTH <= 1) ? 1 : $clog2(READ_DEPTH + 1);
  localparam int unsigned READ_PTR_W = (READ_DEPTH <= 1) ? 1 : $clog2(READ_DEPTH);
  localparam int unsigned WRITE_CNT_W = (WRITE_DEPTH <= 1) ? 1 : $clog2(WRITE_DEPTH + 1);
  localparam int unsigned WRITE_PTR_W = (WRITE_DEPTH <= 1) ? 1 : $clog2(WRITE_DEPTH);
  localparam int unsigned READ_TIMER_W = (READ_LATENCY <= 1) ? 1 : $clog2(READ_LATENCY + 1);
  localparam int unsigned WRITE_TIMER_W = (WRITE_LATENCY <= 1) ? 1 : $clog2(WRITE_LATENCY + 1);

  logic [31:0] read_data_q [READ_DEPTH];
  logic [3:0]  read_id_q [READ_DEPTH];
  logic [READ_TIMER_W-1:0] read_timer_q [READ_DEPTH];
  logic [READ_PTR_W-1:0] read_head;
  logic [READ_PTR_W-1:0] read_tail;
  logic [READ_CNT_W-1:0] read_count;

  logic [3:0] write_id_q [WRITE_DEPTH];
  logic [WRITE_TIMER_W-1:0] write_timer_q [WRITE_DEPTH];
  logic [WRITE_PTR_W-1:0] write_head;
  logic [WRITE_PTR_W-1:0] write_tail;
  logic [WRITE_CNT_W-1:0] write_count;

  logic aw_seen;
  logic [31:0] aw_addr_reg;
  logic [3:0]  aw_id_reg;
  logic w_seen;
  logic [31:0] w_data_reg;
  logic [3:0]  w_strb_reg;

  wire aw_fire = aw_valid && aw_ready;
  wire w_fire = w_valid && w_ready;
  wire ar_fire = ar_valid && ar_ready;
  wire read_room = read_count < READ_DEPTH[READ_CNT_W-1:0];
  wire write_room = write_count < WRITE_DEPTH[WRITE_CNT_W-1:0];
  wire read_reply_fire = r_valid && r_ready;
  wire write_reply_fire = b_valid && b_ready;
  wire read_ready_to_reply = (read_count != '0) && (read_timer_q[read_head] == '0);
  wire write_ready_to_reply = (write_count != '0) && (write_timer_q[write_head] == '0);
  wire read_deq = (!r_valid || r_ready) && read_ready_to_reply;
  wire write_enq = (aw_seen || aw_fire) && (w_seen || w_fire);
  wire write_deq = (!b_valid || b_ready) && write_ready_to_reply;

  assign aw_ready = !reset && write_room && !aw_seen;
  assign w_ready = !reset && write_room && !w_seen;
  assign ar_ready = !reset && read_room;
  assign b_resp = 2'b00;
  assign r_resp = 2'b00;
  assign r_last = 1'b1;

  function automatic [READ_PTR_W-1:0] next_read_ptr(input [READ_PTR_W-1:0] ptr);
    if (READ_DEPTH <= 1) begin
      next_read_ptr = '0;
    end else if (ptr == READ_DEPTH[READ_PTR_W-1:0] - 1'b1) begin
      next_read_ptr = '0;
    end else begin
      next_read_ptr = ptr + 1'b1;
    end
  endfunction

  function automatic [WRITE_PTR_W-1:0] next_write_ptr(input [WRITE_PTR_W-1:0] ptr);
    if (WRITE_DEPTH <= 1) begin
      next_write_ptr = '0;
    end else if (ptr == WRITE_DEPTH[WRITE_PTR_W-1:0] - 1'b1) begin
      next_write_ptr = '0;
    end else begin
      next_write_ptr = ptr + 1'b1;
    end
  endfunction

  function automatic [READ_TIMER_W-1:0] read_delay_value();
    if (READ_LATENCY <= 1) begin
      read_delay_value = '0;
    end else begin
      read_delay_value = READ_LATENCY[READ_TIMER_W-1:0] - 1'b1;
    end
  endfunction

  function automatic [WRITE_TIMER_W-1:0] write_delay_value();
    if (WRITE_LATENCY <= 1) begin
      write_delay_value = '0;
    end else begin
      write_delay_value = WRITE_LATENCY[WRITE_TIMER_W-1:0] - 1'b1;
    end
  endfunction

  integer i;

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
      read_head <= '0;
      read_tail <= '0;
      read_count <= '0;
      write_head <= '0;
      write_tail <= '0;
      write_count <= '0;
      for (i = 0; i < READ_DEPTH; i = i + 1) begin
        read_data_q[i] <= 32'b0;
        read_id_q[i] <= 4'b0;
        read_timer_q[i] <= '0;
      end
      for (i = 0; i < WRITE_DEPTH; i = i + 1) begin
        write_id_q[i] <= 4'b0;
        write_timer_q[i] <= '0;
      end
    end else begin
      if (read_reply_fire) begin
        r_valid <= 1'b0;
      end

      if (write_reply_fire) begin
        b_valid <= 1'b0;
      end

      if (ar_fire) begin
        read_data_q[read_tail] <= npc_pmem_read(ar_addr);
        read_id_q[read_tail] <= ar_id;
        read_timer_q[read_tail] <= read_delay_value();
        read_tail <= next_read_ptr(read_tail);
      end

      for (i = 0; i < READ_DEPTH; i = i + 1) begin
        if (read_timer_q[i] != '0) begin
          read_timer_q[i] <= read_timer_q[i] - 1'b1;
        end
      end

      if (read_deq) begin
        r_valid <= 1'b1;
        r_data <= read_data_q[read_head];
        r_id <= read_id_q[read_head];
        read_head <= next_read_ptr(read_head);
      end

      case ({ar_fire, read_deq})
        2'b10: read_count <= read_count + 1'b1;
        2'b01: read_count <= read_count - 1'b1;
        default: read_count <= read_count;
      endcase

      if (read_deq && read_count == {{(READ_CNT_W-1){1'b0}}, 1'b1}) begin
        read_timer_q[read_head] <= '0;
      end

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

      if (write_enq) begin
        npc_pmem_write(aw_fire ? aw_addr : aw_addr_reg,
                       w_fire ? w_data : w_data_reg,
                       w_fire ? {4'b0, w_strb} : {4'b0, w_strb_reg});
        write_id_q[write_tail] <= aw_fire ? aw_id : aw_id_reg;
        write_timer_q[write_tail] <= write_delay_value();
        write_tail <= next_write_ptr(write_tail);
        aw_seen <= 1'b0;
        w_seen <= 1'b0;
      end

      for (i = 0; i < WRITE_DEPTH; i = i + 1) begin
        if (write_timer_q[i] != '0) begin
          write_timer_q[i] <= write_timer_q[i] - 1'b1;
        end
      end

      if (write_deq) begin
        b_valid <= 1'b1;
        b_id <= write_id_q[write_head];
        write_head <= next_write_ptr(write_head);
      end

      case ({write_enq, write_deq})
        2'b10: write_count <= write_count + 1'b1;
        2'b01: write_count <= write_count - 1'b1;
        default: write_count <= write_count;
      endcase

      if (write_deq && write_count == {{(WRITE_CNT_W-1){1'b0}}, 1'b1}) begin
        write_timer_q[write_head] <= '0;
      end
    end
  end
endmodule
