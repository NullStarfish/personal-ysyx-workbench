#include <am.h>
#include <klib.h>
#include "ysyxsoc.h"

static volatile uint32_t* fb = (volatile uint32_t*)GPU_BASE_ADDR;
static uint32_t w, h;
void __am_gpu_init() {
	w = SCREEN_WIDTH;
	h = SCREEN_HEIGHT;
  //for (int i = 0; i < w * h; i ++) fb[i] = 0;
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
    //自动刷新
  } else {
    if (ctl->w == 0 || ctl->h == 0) return;
    const uint32_t *src = (const uint32_t *)ctl->pixels;
    for (int i = 0; i < ctl->h; i ++) {
      volatile uint32_t *dst = &fb[(ctl->y + i) * w + ctl->x];
      for (int j = 0; j < ctl->w; j ++) {
        dst[j] = src[i * ctl->w + j];
      }
    }
  }

}

void __am_gpu_status(AM_GPU_STATUS_T *status) {
  status->ready = true;
}
