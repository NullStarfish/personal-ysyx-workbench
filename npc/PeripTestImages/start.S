.globl _start
.type _start @function


_start:
    mv s0, x0
    la sp, _stack_top
    j _init
