#include <VTop.h>
#include "verilated.h"
#include "svdpi.h"
enum {
    RESET = 0,
    RUNNING = 1,
    HALTED = 2
};



int npc_state = RESET;


extern "C" int ebreak() {
    // This function is called when an ebreak instruction is executed.
    // You can handle the ebreak here, for example, by printing a message.
    printf("EBREAK instruction executed.\n");
    npc_state = HALTED; // Change the state to HALTED to stop the simulation.
    return 0; // Return 0 to indicate successful handling of ebreak.
}


void single_cycle(VTop* top) {
    top->clk = 0; 
    top->eval();
    top->clk = 1; 
    top->eval();
}





int main() {
    VTop* top = new VTop;
    npc_state = RUNNING;
    while(npc_state == RUNNING && !Verilated::gotFinish()) {
        // Call the single_cycle function to simulate one clock cycle.
      single_cycle(top);
    }
    delete top;
    return 0;
}