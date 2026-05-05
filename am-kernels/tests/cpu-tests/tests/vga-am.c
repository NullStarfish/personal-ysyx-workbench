#include <am.h>
#include <klib.h>
#include <klib-macros.h>

#define W 640
#define H 480

static uint32_t line[W];

static uint32_t rgb(uint8_t r, uint8_t g, uint8_t b) {
  return ((uint32_t)r << 16) | ((uint32_t)g << 8) | b;
}

int main() {
  ioe_init();

  AM_GPU_CONFIG_T cfg = io_read(AM_GPU_CONFIG);
  printf("gpu: present=%d size=%dx%d\n", cfg.present, cfg.width, cfg.height);

  for (int y = 0; y < H; y++) {
    uint32_t color;
    if (y < H / 3) {
      color = rgb(0xff, 0x00, 0x00);
    } else if (y < H * 2 / 3) {
      color = rgb(0x00, 0xff, 0x00);
    } else {
      color = rgb(0x00, 0x00, 0xff);
    }

    for (int x = 0; x < W; x++) {
      line[x] = color;
    }
    io_write(AM_GPU_FBDRAW, 0, y, line, W, 1, false);
  }
  io_write(AM_GPU_FBDRAW, 0, 0, NULL, 0, 0, true);

  printf("vga-am done\n");
  while (1);
}
