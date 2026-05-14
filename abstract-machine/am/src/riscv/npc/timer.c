#include <am.h>
#include "npc.h"
#include "../riscv.h"

void __am_timer_init() {
}

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
  uint32_t lo = inl(RTC_UP_ADDR);
  uint32_t hi = inl(RTC_UP_ADDR + 4);
  uptime->us = ((uint64_t)hi << 32) | lo;
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
  uint32_t lo = inl(RTC_ADDR);
  uint32_t hi = inl(RTC_ADDR + 4);

  rtc->second = lo & 0xff;
  rtc->minute = (lo >> 8) & 0xff;
  rtc->hour   = (lo >> 16) & 0xff;
  rtc->year   = hi & 0xffff;
  rtc->month  = (hi >> 16) & 0xff;
  rtc->day    = (hi >> 24) & 0xff;
}
