#include <am.h>
#include <klib.h>

#define VGA_BASE 0x21000000
#define W 640
#define H 480

static uint32_t rgb(uint8_t r, uint8_t g, uint8_t b) {
  return ((uint32_t)r << 16) | ((uint32_t)g << 8) | b;
}

int main() {
  volatile uint32_t *fb = (volatile uint32_t *)VGA_BASE;

  printf("vga-mmio start\n");
  for (int y = 0; y < H; y++) {
    for (int x = 0; x < W; x++) {
      if (x < W / 3) {
        fb[y * W + x] = rgb(0xff, 0x00, 0x00);
      } else if (x < W * 2 / 3) {
        fb[y * W + x] = rgb(0x00, 0xff, 0x00);
      } else {
        fb[y * W + x] = rgb(0x00, 0x00, 0xff);
      }
    }
  }
  printf("vga-mmio done\n");

  while (1);
}
