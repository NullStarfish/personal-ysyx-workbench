#include <am.h>
#include "ysyxsoc.h"
#include "../riscv.h"

void __am_timer_init() {
}

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
  uint32_t lo = inl(RTC_UP_ADDR);
  uint32_t hi = inl(RTC_UP_ADDR + 4);
  uptime->us = ((uint64_t)hi << 32) | lo;

}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
  rtc->second = 0;
  rtc->minute = 0;
  rtc->hour   = 0;
  rtc->day    = 0;
  rtc->month  = 0;
  rtc->year   = 1900;
  return ;
  // Read the packed RTC words produced by main.cpp:
  // word0 @ RTC_ADDR: (sec) | (min << 6) | (hour << 12)
  // word1 @ SERIAL_PORT+4: (year) | (month << 12) | (day << 16)
  uint32_t w0 = inl(RTC_ADDR);
  uint32_t w1 = inl(RTC_ADDR + 4);

  rtc->second =  w0        & 0x3F;        // bits [5:0]
  rtc->minute = (w0 >> 6)  & 0x3F;        // bits [11:6]
  rtc->hour   = (w0 >> 12) & 0x3F;        // bits [17:12] (enough width)

  rtc->year  =  w1        & 0xFFF;        // bits [11:0], main.cpp stores full year
  rtc->month = (w1 >> 12) & 0xF;          // bits [15:12], 1-12
  rtc->day   = (w1 >> 16) & 0xFF;         // bits [23:16]

  // Defensive bounds (optional)
  if (rtc->second > 59) rtc->second = rtc->second % 60;
  if (rtc->minute > 59) rtc->minute = rtc->minute % 60;
  if (rtc->hour   > 23) rtc->hour   = rtc->hour   % 24;
  if (rtc->month  < 1 || rtc->month > 12) rtc->month = 1;
  if (rtc->day    < 1) rtc->day = 1;
}
