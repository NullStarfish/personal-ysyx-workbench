module CompatibleIMem #(
  parameter int WORDS = 4096
) (
  input  logic [31:0] addr,
  output logic [31:0] rdata
);
  logic [31:0] mem [0:WORDS-1];
  string imem_file;

  initial begin
    for (int i = 0; i < WORDS; i++) begin
      mem[i] = 32'h00000013;
    end

    if ($value$plusargs("IMEM=%s", imem_file)) begin
      $display("[CompatibleIMem] loading %s", imem_file);
      $readmemh(imem_file, mem);
    end else begin
      $display("[CompatibleIMem] no +IMEM=... provided; memory is filled with NOPs");
    end
  end

  always_comb begin
    rdata = mem[addr[31:2]];
  end
endmodule
