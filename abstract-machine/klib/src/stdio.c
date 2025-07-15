#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>
#include <string.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)


// 辅助函数：将整数转为字符串，支持负数
static int int_to_str(int value, char *buf) {
  char tmp[20];
  int i = 0, j = 0, neg = 0;
  if (value < 0) {
    neg = 1;
    value = -value;
  }
  do {
    tmp[i++] = '0' + (value % 10);
    value /= 10;
  } while (value);
  if (neg) tmp[i++] = '-';
  // 逆序写入buf
  while (i--) buf[j++] = tmp[i];
  buf[j] = '\0';
  return j;
}


int printf(const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  char buf[1024];
  int len = vsnprintf(buf, sizeof(buf), fmt, ap);
  va_end(ap);
  for (int i = 0; i < len; i++) {
    putch(buf[i]);
  }
  return len;
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  // 这里假设缓冲区足够大
  return vsnprintf(out, (size_t)-1, fmt, ap);
}


int sprintf(char *out, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int ret = vsprintf(out, fmt, ap);
  va_end(ap);
  return ret;
}


int snprintf(char *out, size_t n, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int ret = vsnprintf(out, n, fmt, ap);
  va_end(ap);
  return ret;
}


int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  size_t out_cnt = 0;
  while (*fmt && out_cnt + 1 < n) { // 预留结尾'\0'
    if (*fmt == '%') {
      fmt++;
      if (*fmt == 's') {
        const char *s = va_arg(ap, const char *);
        while (*s && out_cnt + 1 < n) out[out_cnt++] = *s++;
      } else if (*fmt == 'd') {
        int d = va_arg(ap, int);
        char buf[20];
        int_to_str(d, buf);
        for (int i = 0; buf[i] && out_cnt + 1 < n; i++)
          out[out_cnt++] = buf[i];
      } else if (*fmt) {
        out[out_cnt++] = *fmt;
      }
    } else {
      out[out_cnt++] = *fmt;
    }
    if (*fmt) fmt++;
  }
  out[out_cnt] = '\0';
  return out_cnt;
}



#endif
