// Verilated -*- C++ -*-
// DESCRIPTION: Verilator output: Design implementation internals
// See VTop.h for the primary calling header

#include "verilated.h"
#include "verilated_dpi.h"

#include "VTop_RegFile.h"
#include "VTop__Syms.h"

VL_INLINE_OPT void VTop_RegFile___ico_sequent__TOP__Top__reg_file_unit__0(VTop_RegFile* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+        VTop_RegFile___ico_sequent__TOP__Top__reg_file_unit__0\n"); );
    // Body
    vlSelf->__PVT__DataB = ((0U == ((IData)(vlSymsp->TOP__Top.__VdfgTmp_hff5a8388__0)
                                     ? (0x1fU & (vlSymsp->TOP__Top.__VdfgTmp_hb5448552__0 
                                                 >> 0x14U))
                                     : 0U)) ? 0U : 
                            vlSelf->reg_file[((IData)(vlSymsp->TOP__Top.__VdfgTmp_hff5a8388__0)
                                               ? (0x1fU 
                                                  & (vlSymsp->TOP__Top.__VdfgTmp_hb5448552__0 
                                                     >> 0x14U))
                                               : 0U)]);
}

VL_INLINE_OPT void VTop_RegFile___nba_sequent__TOP__Top__reg_file_unit__0(VTop_RegFile* vlSelf) {
    if (false && vlSelf) {}  // Prevent unused
    VTop__Syms* const __restrict vlSymsp VL_ATTR_UNUSED = vlSelf->vlSymsp;
    VL_DEBUG_IF(VL_DBG_MSGF("+        VTop_RegFile___nba_sequent__TOP__Top__reg_file_unit__0\n"); );
    // Init
    CData/*0:0*/ __Vdlyvset__reg_file__v0;
    __Vdlyvset__reg_file__v0 = 0;
    CData/*4:0*/ __Vdlyvdim0__reg_file__v32;
    __Vdlyvdim0__reg_file__v32 = 0;
    IData/*31:0*/ __Vdlyvval__reg_file__v32;
    __Vdlyvval__reg_file__v32 = 0;
    CData/*0:0*/ __Vdlyvset__reg_file__v32;
    __Vdlyvset__reg_file__v32 = 0;
    // Body
    __Vdlyvset__reg_file__v0 = 0U;
    __Vdlyvset__reg_file__v32 = 0U;
    if (vlSymsp->TOP.rst) {
        __Vdlyvset__reg_file__v0 = 1U;
    } else if (VL_UNLIKELY((((IData)(vlSymsp->TOP__Top.__PVT__RegWEn) 
                             & (0U != ((IData)(vlSymsp->TOP__Top.__VdfgTmp_hff5a8388__0)
                                        ? (0x1fU & 
                                           (vlSymsp->TOP__Top.__VdfgTmp_hb5448552__0 
                                            >> 7U))
                                        : 0U))) & (~ (IData)(vlSymsp->TOP.load_en))))) {
        VL_WRITEF("RegFile write: time=%0t AddrD=%2# DataD=%x\n",
                  64,VL_TIME_UNITED_Q(1),-12,5,((IData)(vlSymsp->TOP__Top.__VdfgTmp_hff5a8388__0)
                                                 ? 
                                                (0x1fU 
                                                 & (vlSymsp->TOP__Top.__VdfgTmp_hb5448552__0 
                                                    >> 7U))
                                                 : 0U),
                  32,((0U == (IData)(vlSymsp->TOP__Top.__PVT__WBSel))
                       ? vlSymsp->TOP__Top.__PVT__dmem_rdata
                       : ((1U == (IData)(vlSymsp->TOP__Top.__PVT__WBSel))
                           ? vlSymsp->TOP__Top.__PVT__alu_result
                           : ((2U == (IData)(vlSymsp->TOP__Top.__PVT__WBSel))
                               ? ((IData)(4U) + vlSymsp->TOP__Top.pc_out)
                               : 0xdeadbeefU))));
        __Vdlyvval__reg_file__v32 = ((0U == (IData)(vlSymsp->TOP__Top.__PVT__WBSel))
                                      ? vlSymsp->TOP__Top.__PVT__dmem_rdata
                                      : ((1U == (IData)(vlSymsp->TOP__Top.__PVT__WBSel))
                                          ? vlSymsp->TOP__Top.__PVT__alu_result
                                          : ((2U == (IData)(vlSymsp->TOP__Top.__PVT__WBSel))
                                              ? ((IData)(4U) 
                                                 + vlSymsp->TOP__Top.pc_out)
                                              : 0xdeadbeefU)));
        __Vdlyvset__reg_file__v32 = 1U;
        __Vdlyvdim0__reg_file__v32 = ((IData)(vlSymsp->TOP__Top.__VdfgTmp_hff5a8388__0)
                                       ? (0x1fU & (vlSymsp->TOP__Top.__VdfgTmp_hb5448552__0 
                                                   >> 7U))
                                       : 0U);
    }
    if (__Vdlyvset__reg_file__v0) {
        vlSelf->reg_file[0U] = 0U;
        vlSelf->reg_file[1U] = 0U;
        vlSelf->reg_file[2U] = 0U;
        vlSelf->reg_file[3U] = 0U;
        vlSelf->reg_file[4U] = 0U;
        vlSelf->reg_file[5U] = 0U;
        vlSelf->reg_file[6U] = 0U;
        vlSelf->reg_file[7U] = 0U;
        vlSelf->reg_file[8U] = 0U;
        vlSelf->reg_file[9U] = 0U;
        vlSelf->reg_file[0xaU] = 0U;
        vlSelf->reg_file[0xbU] = 0U;
        vlSelf->reg_file[0xcU] = 0U;
        vlSelf->reg_file[0xdU] = 0U;
        vlSelf->reg_file[0xeU] = 0U;
        vlSelf->reg_file[0xfU] = 0U;
        vlSelf->reg_file[0x10U] = 0U;
        vlSelf->reg_file[0x11U] = 0U;
        vlSelf->reg_file[0x12U] = 0U;
        vlSelf->reg_file[0x13U] = 0U;
        vlSelf->reg_file[0x14U] = 0U;
        vlSelf->reg_file[0x15U] = 0U;
        vlSelf->reg_file[0x16U] = 0U;
        vlSelf->reg_file[0x17U] = 0U;
        vlSelf->reg_file[0x18U] = 0U;
        vlSelf->reg_file[0x19U] = 0U;
        vlSelf->reg_file[0x1aU] = 0U;
        vlSelf->reg_file[0x1bU] = 0U;
        vlSelf->reg_file[0x1cU] = 0U;
        vlSelf->reg_file[0x1dU] = 0U;
        vlSelf->reg_file[0x1eU] = 0U;
        vlSelf->reg_file[0x1fU] = 0U;
    }
    if (__Vdlyvset__reg_file__v32) {
        vlSelf->reg_file[__Vdlyvdim0__reg_file__v32] 
            = __Vdlyvval__reg_file__v32;
    }
}
