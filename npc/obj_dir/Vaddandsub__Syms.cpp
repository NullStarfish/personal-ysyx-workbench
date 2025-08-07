// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Symbol table implementation internals

#include "Vaddandsub__Syms.h"
#include "Vaddandsub.h"
#include "Vaddandsub___024root.h"
#include "Vaddandsub_Top.h"
#include "Vaddandsub___024unit.h"
#include "Vaddandsub_IMEM.h"
#include "Vaddandsub_rom.h"

// FUNCTIONS
Vaddandsub__Syms::~Vaddandsub__Syms()
{
}

Vaddandsub__Syms::Vaddandsub__Syms(VerilatedContext* contextp, const char* namep, Vaddandsub* modelp)
    : VerilatedSyms{contextp}
    // Setup internal state of the Syms class
    , __Vm_modelp{modelp}
    // Setup module instances
    , TOP{this, namep}
    , TOP__Top{this, Verilated::catName(namep, "Top")}
    , TOP__Top__imem_unit{this, Verilated::catName(namep, "Top.imem_unit")}
    , TOP__Top__imem_unit__rom0{this, Verilated::catName(namep, "Top.imem_unit.rom0")}
    , TOP____024unit{this, Verilated::catName(namep, "$unit")}
{
    // Configure time unit / time precision
    _vm_contextp__->timeunit(-12);
    _vm_contextp__->timeprecision(-12);
    // Setup each module's pointers to their submodules
    TOP.Top = &TOP__Top;
    TOP__Top.imem_unit = &TOP__Top__imem_unit;
    TOP__Top__imem_unit.rom0 = &TOP__Top__imem_unit__rom0;
    TOP.__PVT____024unit = &TOP____024unit;
    // Setup each module's pointer back to symbol table (for public functions)
    TOP.__Vconfigure(true);
    TOP__Top.__Vconfigure(true);
    TOP__Top__imem_unit.__Vconfigure(true);
    TOP__Top__imem_unit__rom0.__Vconfigure(true);
    TOP____024unit.__Vconfigure(true);
    // Setup scopes
    __Vscope_Top__imem_unit__rom0.configure(this, name(), "Top.imem_unit.rom0", "rom0", 0, VerilatedScope::SCOPE_OTHER);
    // Setup export functions
    for (int __Vfinal = 0; __Vfinal < 2; ++__Vfinal) {
        __Vscope_Top__imem_unit__rom0.varInsert(__Vfinal,"rom_data", &(TOP__Top__imem_unit__rom0.rom_data), false, VLVT_UINT32,VLVD_NODIR|VLVF_PUB_RW,2 ,31,0 ,0,32767);
    }
}
