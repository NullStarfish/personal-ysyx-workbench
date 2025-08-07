// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Model implementation (design independent parts)

#include "Vaddandsub.h"
#include "Vaddandsub__Syms.h"
#include "verilated_dpi.h"

//============================================================
// Constructors

Vaddandsub::Vaddandsub(VerilatedContext* _vcontextp__, const char* _vcname__)
    : VerilatedModel{*_vcontextp__}
    , vlSymsp{new Vaddandsub__Syms(contextp(), _vcname__, this)}
    , clk{vlSymsp->TOP.clk}
    , rst{vlSymsp->TOP.rst}
    , BrUn{vlSymsp->TOP.BrUn}
    , BrEq{vlSymsp->TOP.BrEq}
    , BrLT{vlSymsp->TOP.BrLT}
    , rs1{vlSymsp->TOP.rs1}
    , rs2{vlSymsp->TOP.rs2}
    , Top{vlSymsp->TOP.Top}
    , __PVT____024unit{vlSymsp->TOP.__PVT____024unit}
    , rootp{&(vlSymsp->TOP)}
{
    // Register model with the context
    contextp()->addModel(this);
}

Vaddandsub::Vaddandsub(const char* _vcname__)
    : Vaddandsub(Verilated::threadContextp(), _vcname__)
{
}

//============================================================
// Destructor

Vaddandsub::~Vaddandsub() {
    delete vlSymsp;
}

//============================================================
// Evaluation function

#ifdef VL_DEBUG
void Vaddandsub___024root___eval_debug_assertions(Vaddandsub___024root* vlSelf);
#endif  // VL_DEBUG
void Vaddandsub___024root___eval_static(Vaddandsub___024root* vlSelf);
void Vaddandsub___024root___eval_initial(Vaddandsub___024root* vlSelf);
void Vaddandsub___024root___eval_settle(Vaddandsub___024root* vlSelf);
void Vaddandsub___024root___eval(Vaddandsub___024root* vlSelf);

void Vaddandsub::eval_step() {
    VL_DEBUG_IF(VL_DBG_MSGF("+++++TOP Evaluate Vaddandsub::eval_step\n"); );
#ifdef VL_DEBUG
    // Debug assertions
    Vaddandsub___024root___eval_debug_assertions(&(vlSymsp->TOP));
#endif  // VL_DEBUG
    vlSymsp->__Vm_deleter.deleteAll();
    if (VL_UNLIKELY(!vlSymsp->__Vm_didInit)) {
        vlSymsp->__Vm_didInit = true;
        VL_DEBUG_IF(VL_DBG_MSGF("+ Initial\n"););
        Vaddandsub___024root___eval_static(&(vlSymsp->TOP));
        Vaddandsub___024root___eval_initial(&(vlSymsp->TOP));
        Vaddandsub___024root___eval_settle(&(vlSymsp->TOP));
    }
    // MTask 0 start
    VL_DEBUG_IF(VL_DBG_MSGF("MTask0 starting\n"););
    Verilated::mtaskId(0);
    VL_DEBUG_IF(VL_DBG_MSGF("+ Eval\n"););
    Vaddandsub___024root___eval(&(vlSymsp->TOP));
    // Evaluate cleanup
    Verilated::endOfThreadMTask(vlSymsp->__Vm_evalMsgQp);
    Verilated::endOfEval(vlSymsp->__Vm_evalMsgQp);
}

//============================================================
// Events and timing
bool Vaddandsub::eventsPending() { return false; }

uint64_t Vaddandsub::nextTimeSlot() {
    VL_FATAL_MT(__FILE__, __LINE__, "", "%Error: No delays in the design");
    return 0;
}

//============================================================
// Utilities

const char* Vaddandsub::name() const {
    return vlSymsp->name();
}

//============================================================
// Invoke final blocks

void Vaddandsub___024root___eval_final(Vaddandsub___024root* vlSelf);

VL_ATTR_COLD void Vaddandsub::final() {
    Vaddandsub___024root___eval_final(&(vlSymsp->TOP));
}

//============================================================
// Implementations of abstract methods from VerilatedModel

const char* Vaddandsub::hierName() const { return vlSymsp->name(); }
const char* Vaddandsub::modelName() const { return "Vaddandsub"; }
unsigned Vaddandsub::threads() const { return 1; }
