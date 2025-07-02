`timescale 1ns/1ps

module testbench;

    // --- Signals ---
    reg clk;
    reg rst;

    // Instantiate the Unit Under Test (UUT)
    Top uut (
        .clk(clk),
        .rst(rst)
    );

    // --- Clock Generation ---
    initial begin
        clk = 0;
        forever #5 clk = ~clk; // 10ns clock period (100 MHz)
    end

    // --- Test Sequence ---
    initial begin
        $dumpfile("testbench.vcd");
        $dumpvars(0, testbench);
        // 1. Initialize and apply reset
        rst = 1;
        $display("T=%0t: --- System Reset Asserted ---", $time);
        #20; // Hold reset for 2 cycles

        rst = 0;
        $display("T=%0t: --- System Reset De-asserted ---", $time);
        
        // 2. Monitor execution
        // We will stop simulation when the processor halts by jumping to itself.
        // The test program uses 'jalr x0, x0, 0' at address 0x20 which will cause
        // the PC to get stuck at 0x20. We can monitor for this condition.
    end

    // --- Monitoring ---
    // Use $monitor to print signals when they change.
    initial begin
        // Wait for reset to de-assert before starting to monitor
        #20;
        $monitor("T=%0t: PC=%h, Inst=%h, RegWEn=%b, rd=%d, WBData=%h",
                 $time, uut.pc_out, uut.inst, uut.RegWEn, uut.rd, uut.wb_data);
    end

    // --- Simulation Termination ---
    always @(posedge clk) begin
        // Terminate after some time or on a halt condition
        if ($time > 500) begin
            $display("T=%0t: Simulation timed out. Stopping.", $time);
            $finish;
        end

        // Halt condition: PC is 0x20 and instruction is 'jalr x0, x0, 0' (0x00008067)
        // This means the test program has completed.
        if (uut.pc_out == 32'h00000020 && uut.inst == 32'h00008067) begin
            #30; // Wait a few cycles to see the final state
            $display("T=%0t: --- Test Program HALTED ---", $time);
            // Display final state of relevant registers
            $display("Final Register States:");
            $display("x5 (should be 0xAAAAA555) = %h", uut.reg_file_unit.reg_file[5]);
            $display("x6 (should be 100)        = %d", uut.reg_file_unit.reg_file[6]);
            $display("x7 (should be 0xAAAAA555) = %h", uut.reg_file_unit.reg_file[7]);
            $display("x2 (should be 2)          = %d", uut.reg_file_unit.reg_file[2]);
            $finish;
        end
    end

endmodule
