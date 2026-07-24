#include <amdev.h>
#include <klib-macros.h>

//void ioe_read(int reg, void *buf);

AM_INPUT_KEYBRD_T read_key(void) {
  return io_read(AM_INPUT_KEYBRD);
}