riscv64-linux-gnu-gcc \
  -march=rv32e -mabi=ilp32e \
  -ffreestanding \
  -fno-pic -fno-pie -no-pie \
  -fno-ident \
  -mno-riscv-attribute \
  -Wa,-mno-arch-attr \
  -fno-unwind-tables \
  -fno-asynchronous-unwind-tables \
  -nostdlib \
  -Wl,--build-id=none \
  -Wl,-T,linker.ld \
  start.S _init.c uart.c \
  -o new.elf