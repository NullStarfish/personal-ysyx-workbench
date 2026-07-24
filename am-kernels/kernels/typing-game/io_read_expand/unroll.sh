riscv64-linux-gnu-gcc -E -P -std=gnu11 \
  -I$AM_HOME/am/include -I$AM_HOME/klib/include \
  io_read_demo.c -o io_read_demo.i