#include <am.h>
#include "ysyxsoc.h"
#include "../riscv.h"

void __am_timer_init() {
}

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
  uint32_t hi1, lo, hi2;

  do {
    hi1 = inl(RTC_UP_ADDR + 4);
    lo  = inl(RTC_UP_ADDR);
    hi2 = inl(RTC_UP_ADDR + 4);
  } while (hi1 != hi2);

  uint64_t ticks = ((uint64_t)hi1 << 32) | lo;
  uptime->us = ticks;
}

static bool is_leap_year(int year) {
  return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
}

static int days_in_month(int year, int month) {
  static const int days[] = {
    31, 28, 31, 30, 31, 30,
    31, 31, 30, 31, 30, 31
  };

  if (month == 2 && is_leap_year(year)) return 29;
  return days[month - 1];
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
  AM_TIMER_UPTIME_T uptime;
  __am_timer_uptime(&uptime);

  uint64_t seconds = uptime.us / 1000000;
  rtc->second = seconds % 60;
  seconds /= 60;
  rtc->minute = seconds % 60;
  seconds /= 60;
  rtc->hour = seconds % 24;
  uint64_t days = seconds / 24;

  int year = 2026;
  int month = 1;
  while (days >= (uint64_t)(is_leap_year(year) ? 366 : 365)) {
    days -= is_leap_year(year) ? 366 : 365;
    year++;
  }

  while (days >= (uint64_t)days_in_month(year, month)) {
    days -= days_in_month(year, month);
    month++;
  }

  rtc->day = days + 1;
  rtc->month = month;
  rtc->year = year;
}
