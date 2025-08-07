// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Symbol table internal header
//
// Internal details; most calling programs do not need this header,
// unless using verilator public meta comments.

#ifndef VERILATED_VADDANDSUB__SYMS_H_
#define VERILATED_VADDANDSUB__SYMS_H_  // guard

#include "verilated.h"

// INCLUDE MODEL CLASS

#include "Vaddandsub.h"

// INCLUDE MODULE CLASSES
#include "Vaddandsub___024root.h"
#include "Vaddandsub_Top.h"
#include "Vaddandsub___024unit.h"
#include "Vaddandsub_IMEM.h"
#include "Vaddandsub_rom.h"

// DPI TYPES for DPI Export callbacks (Internal use)

// SYMS CLASS (contains all model state)
class Vaddandsub__Syms final : public VerilatedSyms {
  public:
    // INTERNAL STATE
    Vaddandsub* const __Vm_modelp;
    VlDeleter __Vm_deleter;
    bool __Vm_didInit = false;

    // MODULE INSTANCE STATE
    Vaddandsub___024root           TOP;
    Vaddandsub_Top                 TOP__Top;
    Vaddandsub_IMEM                TOP__Top__imem_unit;
    Vaddandsub_rom                 TOP__Top__imem_unit__rom0;
    Vaddandsub___024unit           TOP____024unit;

    // SCOPE NAMES
    VerilatedScope __Vscope_Top__imem_unit__rom0;

    // CONSTRUCTORS
    Vaddandsub__Syms(VerilatedContext* contextp, const char* namep, Vaddandsub* modelp);
    ~Vaddandsub__Syms();

    // METHODS
    const char* name() { return TOP.name(); }
} VL_ATTR_ALIGNED(VL_CACHE_LINE_BYTES);

#endif  // guard
