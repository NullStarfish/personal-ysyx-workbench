// interfaces.sv
// Defines all handshake and feedback interfaces used in the CPU.

// Generic stage-to-stage handshake interface
interface stage_if #(parameter type PAYLOAD = logic [31:0]);
    PAYLOAD      payload;
    logic        valid;
    logic        ready;

    modport master (output payload, output valid, input ready);
    modport slave  (input payload, input valid, output ready);

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