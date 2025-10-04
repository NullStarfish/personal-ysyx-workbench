// cpu_types_pkg.sv
// Defines all data structures (payloads) passed between stages.
package cpu_types_pkg;

    // Enumerations for control signals
    typedef enum logic [3:0] {
        ALU_ADD, ALU_SUB, ALU_AND, ALU_OR, ALU_XOR,
        ALU_SLT, ALU_SLTU, ALU_SLL, ALU_SRL, ALU_SRA,
        ALU_COPY_A, ALU_COPY_B
    } alusel_e;

    typedef enum logic [1:0] {
        CSR_NONE,  // Not a CSR operation
        CSR_WRITE, // CSRRW, CSRRWI
        CSR_SET,   // CSRRS, CSRRSI
        CSR_CLEAR  // CSRRC, CSRRCI
    } csr_op_e;


    typedef enum logic [2:0] {
        IMM_I, IMM_S, IMM_B, IMM_U, IMM_J
    } immsel_e;

    // Payload from IFU to IDU
    typedef struct packed {
        logic [31:0] inst;
        logic [31:0] pc;


        logic valid; // Indicates if the data is valid
    } if_id_t;

    // Payload from IDU to EXU
    typedef struct packed {

        //四个主要数据源,发送到alu
        //rs1和rs2同时也经过BranchComp，决定输出pc+4还是pc + imm
        logic [31:0] pc;
        logic [31:0] rs1_data;
        logic pc_rs1_sel;

        logic [31:0] rs2_data;
        logic [31:0] imm;
        logic rs2_imm_sel;


        logic reg_wen;       // Register Write Enable
        logic [4:0]  rd_addr;       // Destination Register Address

        alusel_e alu_opcode; // ALU operation code


        logic pc_redirect;//jal，branch等跳转指令需要改写pc，直接在idu中解码    
        logic forjal;//jal指令需要pc+4写入rd

        logic        mem_en;// Memory Access Enable
        logic        mem_wen;// Memory Write Enable
        logic [2:0]  funct3;//用于load/store解码

        logic valid; // Indicates if the data is valid

        logic        is_ecall;
        logic        is_mret;
        logic        is_ebreak; // [NEW] Signal for EBREAK instruction
        logic        [31:0] a5_data;


        csr_op_e     csr_op;       // Type of CSR operation
        logic [11:0] csr_addr;
        logic [31:0] rs1_or_imm; // Pass rs1 data or immediate for CSR ops
    } id_ex_t;

    // Payload from EXU to LSU
    typedef struct packed {
        // --- For LSU (Memory Access) ---
        //若mem_en为0，mem仅做转发
        logic        mem_en;         
        logic        mem_wen;        
        logic [31:0] mem_wdata;
        logic [31:0] mem_addr;//for both read and write, also suitable for atomic operation 
        logic [2:0]  funct3; //用于load/store解码     

        // --- For WBU (Write-Back) ---
        logic [31:0] exu_result;        // Data for register write-back (ALU result or PC+4)
        logic [4:0]  rd_addr;        // Destination register address
        logic        reg_wen;        // Register write enable
        logic [31:0] pc_target;    //为了有jal，wbu需要同时写pc和reg

        //jal中pc+4写入rd，pc+imm写入pc，
        //在exu中同时存在pc + imm和pc + 4，只是一般来说，pc_target为pc + 4, 


        logic valid;

    } ex_lsu_t; // We give it a descriptive name




    // Payload from LSU to WBU
    typedef struct packed {
        logic [31:0] wb_data;
        logic [4:0]  rd_addr;
        logic        reg_wen;
        logic [31:0] pc_target; // Pass through for redirect target


        logic valid;

    } lsu_wb_t;

endpackage