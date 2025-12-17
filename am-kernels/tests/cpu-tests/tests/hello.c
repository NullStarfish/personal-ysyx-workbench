#include "klib.h"
#include "am.h"
#include <klib-macros.h>


//#define __ISA__ "riscv32"
int main() {
  for (int i = 0; i < 10; i ++) {
    putstr("Hello, AM World @  riscv32 \n");
  }
}