module tb_CourseCompatibleTop;
  logic clock;
  logic reset;
  logic [31:0] debug_dmem_addr;
  wire [1023:0] debug_regs_flat;
  wire [31:0] debug_pc;
  wire [7:0] debug_dmem_byte;
  wire retire_valid;
  wire [31:0] retire_pc;
  wire [31:0] retire_inst;

  string test_name;
  string dump_file;
  int max_cycles;
  int cycle;
  int wave_start;
  int wave_end;
  bit enable_wave;

  CourseCompatibleTop dut (
    .clock(clock),
    .reset(reset),
    .io_debug_regs_flat(debug_regs_flat),
    .io_debug_pc(debug_pc),
    .io_debug_dmem_addr(debug_dmem_addr),
    .io_debug_dmem_byte(debug_dmem_byte),
    .io_retire_valid(retire_valid),
    .io_retire_pc(retire_pc),
    .io_retire_inst(retire_inst)
  );

  initial begin
    clock = 1'b0;
    forever #5 clock = ~clock;
  end

  function automatic logic [31:0] reg_value(input int idx);
    reg_value = debug_regs_flat[idx * 32 +: 32];
  endfunction

  task automatic expect_reg(input int idx, input logic [31:0] expected);
    logic [31:0] actual;
    begin
      actual = reg_value(idx);
      if (actual !== expected) begin
        $fatal(1, "[%s] x%0d mismatch: expected 0x%08x, got 0x%08x", test_name, idx, expected, actual);
      end
    end
  endtask

  task automatic expect_mem_byte(input logic [31:0] addr, input logic [7:0] expected);
    begin
      debug_dmem_addr = addr;
      #1;
      if (debug_dmem_byte !== expected) begin
        $fatal(1, "[%s] mem[0x%08x] mismatch: expected 0x%02x, got 0x%02x",
               test_name, addr, expected, debug_dmem_byte);
      end
    end
  endtask

  task automatic check_expectations;
    begin
      if (test_name == "smoke") begin
        expect_reg(3, 32'd12);
        expect_reg(4, 32'd12);
        expect_reg(5, 32'd9);
        expect_reg(8, 32'd1);
        expect_mem_byte(32'd0, 8'd12);
        expect_mem_byte(32'd1, 8'd1);
      end else if (test_name == "hazard_raw") begin
        expect_reg(5, 32'd16);
      end else if (test_name == "hazard_load_use") begin
        expect_reg(2, 32'd17);
        expect_reg(3, 32'd34);
      end else if (test_name == "hazard_flush") begin
        expect_reg(2, 32'd9);
        expect_reg(3, 32'h00000014);
        expect_reg(4, 32'd7);
      end else if (test_name == "program_arith_upper") begin
        expect_reg(1, 32'h12345000);
        expect_reg(2, 32'd16);
        expect_reg(4, 32'h12345000);
        expect_reg(8, 32'h12345000);
      end else if (test_name == "program_mem_byte_half") begin
        expect_reg(2, 32'h000000ab);
        expect_reg(3, 32'h00000234);
        expect_mem_byte(32'd1, 8'hab);
        expect_mem_byte(32'd2, 8'h34);
        expect_mem_byte(32'd3, 8'h02);
      end else if (test_name == "rv32i_upper_jump") begin
        expect_reg(1, 32'h12345000);
        expect_reg(2, 32'h00001004);
        expect_reg(3, 32'hffffffff);
        expect_reg(4, 32'd1);
        expect_reg(5, 32'd1);
        expect_reg(6, 32'hffffff00);
        expect_reg(7, 32'h00000055);
        expect_reg(8, 32'h00000005);
        expect_reg(9, 32'h00000550);
        expect_reg(10, 32'h00000055);
        expect_reg(11, 32'hffffffff);
        expect_reg(12, 32'h00000030);
        expect_reg(13, 32'd0);
        expect_reg(14, 32'h00000044);
        expect_reg(16, 32'h00000044);
        expect_reg(17, 32'd0);
      end else if (test_name == "rv32i_alu_branch") begin
        expect_reg(5, 32'd10);
        expect_reg(6, 32'd4);
        expect_reg(7, 32'd56);
        expect_reg(8, 32'd1);
        expect_reg(9, 32'd0);
        expect_reg(10, 32'd4);
        expect_reg(11, 32'h7fffffff);
        expect_reg(12, 32'hffffffff);
        expect_reg(13, 32'd7);
        expect_reg(14, 32'd3);
        expect_reg(20, 32'd0);
        expect_reg(21, 32'd1);
        expect_reg(22, 32'd0);
        expect_reg(23, 32'd0);
        expect_reg(24, 32'd0);
        expect_reg(25, 32'd0);
        expect_reg(26, 32'd0);
      end else if (test_name == "rv32i_load_store") begin
        expect_reg(2, 32'hffffff80);
        expect_reg(3, 32'h00000080);
        expect_reg(5, 32'hffff8001);
        expect_reg(6, 32'h00008001);
        expect_reg(8, 32'h12345078);
        expect_mem_byte(32'd0, 8'h80);
        expect_mem_byte(32'd2, 8'h01);
        expect_mem_byte(32'd3, 8'h80);
        expect_mem_byte(32'd4, 8'h78);
        expect_mem_byte(32'd5, 8'h50);
        expect_mem_byte(32'd6, 8'h34);
        expect_mem_byte(32'd7, 8'h12);
      end else begin
        $fatal(1, "unknown +TEST=%s", test_name);
      end
    end
  endtask

  initial begin
    if (!$value$plusargs("TEST=%s", test_name)) begin
      test_name = "smoke";
    end
    if (!$value$plusargs("MAX_CYCLES=%d", max_cycles)) begin
      max_cycles = 200;
    end
    enable_wave = $value$plusargs("DUMPFILE=%s", dump_file);
    if (!$value$plusargs("WAVE_START=%d", wave_start)) begin
      wave_start = 0;
    end
    if (!$value$plusargs("WAVE_END=%d", wave_end)) begin
      wave_end = max_cycles;
    end

    if (enable_wave) begin
      $dumpfile(dump_file);
      $dumpvars(0, tb_CourseCompatibleTop);
      if (wave_start > 0) begin
        $dumpoff;
      end
    end

    debug_dmem_addr = 32'b0;
    reset = 1'b1;
    repeat (5) @(posedge clock);
    reset = 1'b0;

    for (cycle = 0; cycle < max_cycles; cycle++) begin
      @(posedge clock);
      if (enable_wave && cycle == wave_start) begin
        $dumpon;
      end
      if (enable_wave && cycle == wave_end) begin
        $dumpoff;
      end
      if (retire_valid && retire_inst == 32'h00100073) begin
        $display("[%s] ebreak retired at cycle %0d, pc=0x%08x", test_name, cycle, retire_pc);
        check_expectations();
        $display("[%s] PASS", test_name);
        $finish;
      end
    end

    $fatal(1, "[%s] timeout after %0d cycles, debug_pc=0x%08x", test_name, max_cycles, debug_pc);
  end
endmodule
