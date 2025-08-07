# Verilated -*- Makefile -*-
# DESCRIPTION: Verilator output: Make include file with class lists
#
# This file lists generated Verilated files, for including in higher level makefiles.
# See Vaddandsub.mk for the caller.

### Switches...
# C11 constructs required?  0/1 (always on now)
VM_C11 = 1
# Timing enabled?  0/1
VM_TIMING = 0
# Coverage output mode?  0/1 (from --coverage)
VM_COVERAGE = 0
# Parallel builds?  0/1 (from --output-split)
VM_PARALLEL_BUILDS = 0
# Tracing output mode?  0/1 (from --trace/--trace-fst)
VM_TRACE = 0
# Tracing output mode in VCD format?  0/1 (from --trace)
VM_TRACE_VCD = 0
# Tracing output mode in FST format?  0/1 (from --trace-fst)
VM_TRACE_FST = 0

### Object file lists...
# Generated module classes, fast-path, compile with highest optimization
VM_CLASSES_FAST += \
	Vaddandsub \
	Vaddandsub___024root__DepSet_h25fadda4__0 \
	Vaddandsub___024root__DepSet_h8fb1d1a7__0 \
	Vaddandsub_Top__DepSet_h70e3ff70__0 \
	Vaddandsub_Top__DepSet_h95a22b45__0 \
	Vaddandsub___024unit__DepSet_h45e0dd52__0 \
	Vaddandsub_rom__DepSet_h3e77fd52__0 \

# Generated module classes, non-fast-path, compile with low/medium optimization
VM_CLASSES_SLOW += \
	Vaddandsub___024root__Slow \
	Vaddandsub___024root__DepSet_h25fadda4__0__Slow \
	Vaddandsub___024root__DepSet_h8fb1d1a7__0__Slow \
	Vaddandsub_Top__Slow \
	Vaddandsub_Top__DepSet_h70e3ff70__0__Slow \
	Vaddandsub___024unit__Slow \
	Vaddandsub___024unit__DepSet_hafa7d0b5__0__Slow \
	Vaddandsub_IMEM__Slow \
	Vaddandsub_IMEM__DepSet_ha87339f4__0__Slow \
	Vaddandsub_rom__Slow \
	Vaddandsub_rom__DepSet_h883eb0b5__0__Slow \

# Generated support classes, fast-path, compile with highest optimization
VM_SUPPORT_FAST += \
	Vaddandsub__Dpi \

# Generated support classes, non-fast-path, compile with low/medium optimization
VM_SUPPORT_SLOW += \
	Vaddandsub__Syms \

# Global classes, need linked once per executable, fast-path, compile with highest optimization
VM_GLOBAL_FAST += \
	verilated \
	verilated_dpi \
	verilated_threads \

# Global classes, need linked once per executable, non-fast-path, compile with low/medium optimization
VM_GLOBAL_SLOW += \


# Verilated -*- Makefile -*-
