module PC (
    input clk, //clock signal
    input rst, //reset signal
    input PCsel, //use to select the PC source
    input [31:0] ALUin, //input to the PC
    output reg [31:0] pc, //output PC value
    output [31:0] PCplus4
);
     //PC+4 value
    wire [31:0] PCin; //input to the PC
    assign PCin = PCsel ? ALUin : PCplus4; //select the input to the PC based on PCsel
    
    always@(posedge clk) begin
        if (rst) //if reset is high, set PC to the program start address
            pc <= 32'h80000000;
        else //otherwise, update the PC value
            pc <= PCin; //update the PC value on the positive edge of PCsel
    end
    
    assign PCplus4 = pc + 4; //calculate PC+4 value
endmodule
