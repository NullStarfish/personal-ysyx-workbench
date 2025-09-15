#include <am.h>
#include <nemu.h>
#include <klib.h>

#define SYNC_ADDR (VGACTL_ADDR + 4)



static uint32_t w, h;
static uint32_t *fb;

void __am_gpu_init() {
  uint32_t size = inl(VGACTL_ADDR);
  w = size >> 16;
  h = size & 0xffff;
  fb = (uint32_t *)(uintptr_t)FB_ADDR;
  assert(fb != NULL);
  for (int i = 0; i < w * h; i ++) fb[i] = 0;
  outl(SYNC_ADDR, 1);

}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = w, .height = h,
    .vmemsz = w * h * sizeof(uint32_t)
  };
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  if (ctl->sync) {
    outl(SYNC_ADDR, 1);
    //当用户程序给了sync高电平之后，给nemu一个高电平，表示同步完成
  } else {
    //为了让程序记住每个矩形，我们需要一个显存,也就是fb
    for (int i = 0; i < ctl->h; i ++) {
      memcpy(&fb[(ctl->y + i) * w + ctl->x], 
             &((uint32_t *)ctl->pixels)[i * ctl->w], 
             ctl->w * sizeof(uint32_t));
    }
  }

}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
