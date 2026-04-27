module CompatibleDMem #(
  parameter int WORDS = 4096
) (
  input  logic        clock,
  input  logic [31:0] addr,
  input  logic        ren,
  input  logic        wen,
  input  logic [2:0]  subop,
  input  logic        unsignedLoad,
  input  logic [31:0] wdata,
  output logic [31:0] rdata,
  input  logic [31:0] debugAddr,
  output logic [7:0]  debugByte
);
  localparam logic [2:0] SUBOP_BYTE = 3'b001;
  localparam logic [2:0] SUBOP_HALF = 3'b010;
  localparam logic [2:0] SUBOP_WORD = 3'b011;

  logic [31:0] mem [0:WORDS-1];
  string dmem_file;

  function automatic logic [7:0] read_byte(input logic [31:0] byte_addr);
    logic [31:0] word;
    begin
      word = mem[byte_addr[31:2]];
      unique case (byte_addr[1:0])
        2'd0: read_byte = word[7:0];
        2'd1: read_byte = word[15:8];
        2'd2: read_byte = word[23:16];
        default: read_byte = word[31:24];
      endcase
    end
  endfunction

  initial begin
    for (int i = 0; i < WORDS; i++) begin
      mem[i] = 32'h00000000;
    end

    if ($value$plusargs("DMEM=%s", dmem_file)) begin
      $display("[CompatibleDMem] loading %s", dmem_file);
      $readmemh(dmem_file, mem);
    end
  end

  always_comb begin
    logic [7:0] byte_value;
    logic [15:0] half_value;
    byte_value = read_byte(addr);
    half_value = {read_byte({addr[31:1], 1'b1}), read_byte({addr[31:1], 1'b0})};

    unique case (subop)
      SUBOP_BYTE: rdata = unsignedLoad ? {24'b0, byte_value} : {{24{byte_value[7]}}, byte_value};
      SUBOP_HALF: rdata = unsignedLoad ? {16'b0, half_value} : {{16{half_value[15]}}, half_value};
      default:    rdata = mem[addr[31:2]];
    endcase

    if (!ren) begin
      rdata = 32'b0;
    end

    debugByte = read_byte(debugAddr);
  end

  always_ff @(posedge clock) begin
    if (wen) begin
      unique case (subop)
        SUBOP_BYTE: begin
          unique case (addr[1:0])
            2'd0: mem[addr[31:2]][7:0] <= wdata[7:0];
            2'd1: mem[addr[31:2]][15:8] <= wdata[7:0];
            2'd2: mem[addr[31:2]][23:16] <= wdata[7:0];
            default: mem[addr[31:2]][31:24] <= wdata[7:0];
          endcase
        end
        SUBOP_HALF: begin
          if (addr[1]) begin
            mem[addr[31:2]][31:16] <= wdata[15:0];
          end else begin
            mem[addr[31:2]][15:0] <= wdata[15:0];
          end
        end
        SUBOP_WORD: begin
          mem[addr[31:2]] <= wdata;
        end
        default: begin
        end
      endcase
    end
  end
endmodule
