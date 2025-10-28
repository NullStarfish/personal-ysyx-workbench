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

// 新增：long 转字符串，支持负数
static int long_to_str(long value, char *buf) {
  char tmp[32];
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
  while (i--) buf[j++] = tmp[i];
  buf[j] = '\0';
  return j;
}

// 新增：无符号长整型转十六进制字符串（小写），不带"0x"
static int ulong_to_hex(unsigned long value, char *buf) {
  char tmp[32];
  const char *hex = "0123456789abcdef";
  int i = 0, j = 0;
  if (value == 0) {
    buf[0] = '0';
    buf[1] = '\0';
    return 1;
  }
  while (value) {
    tmp[i++] = hex[value & 0xF];
    value >>= 4;
  }
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
      // Parse optional zero-padding flag and width like %02d
      bool zero_pad = false;
      int width = 0;
      if (*fmt == '0') { zero_pad = true; fmt++; }
      while (*fmt >= '0' && *fmt <= '9') { width = width * 10 + (*fmt - '0'); fmt++; }

      if (*fmt == 's') {
        const char *s = va_arg(ap, const char *);
        while (*s && out_cnt + 1 < n) out[out_cnt++] = *s++;
        fmt++;
        continue;
      } else if (*fmt == 'd') {
        int d = va_arg(ap, int);
        char buf[32];
        int_to_str(d, buf);
        int len = (int)strlen(buf);
        int sign = (buf[0] == '-');
        if (sign && zero_pad) {
          // print sign first
          if (out_cnt + 1 < n) out[out_cnt++] = '-';
          const char *num = buf + 1;
          int num_len = len - 1;
          int pad = (width > len) ? (width - len) : 0;
          for (int i = 0; i < pad && out_cnt + 1 < n; i++) out[out_cnt++] = '0';
          for (int i = 0; i < num_len && out_cnt + 1 < n; i++) out[out_cnt++] = num[i];
        } else {
          int pad = (width > len) ? (width - len) : 0;
          char padch = zero_pad ? '0' : ' ';
          for (int i = 0; i < pad && out_cnt + 1 < n; i++) out[out_cnt++] = padch;
          for (int i = 0; i < len && out_cnt + 1 < n; i++) out[out_cnt++] = buf[i];
        }
        fmt++;
        continue;
      } else if (*fmt == 'x') {
        unsigned int x = va_arg(ap, unsigned int);
        char buf[32];
        ulong_to_hex((unsigned long)x, buf);
        int len = (int)strlen(buf);
        int pad = (width > len) ? (width - len) : 0;
        char padch = zero_pad ? '0' : ' ';
        for (int i = 0; i < pad && out_cnt + 1 < n; i++) out[out_cnt++] = padch;
        for (int i = 0; buf[i] && out_cnt + 1 < n; i++) out[out_cnt++] = buf[i];
        fmt++;
        continue;
      } else if (*fmt == 'p') {
        void *p = va_arg(ap, void *);
        unsigned long addr = (unsigned long)p;
        char buf[32];
        ulong_to_hex(addr, buf);
        // "0x" prefix
        if (out_cnt + 2 < n) {
          out[out_cnt++] = '0';
          out[out_cnt++] = 'x';
        }
        int len = (int)strlen(buf);
        int pad = (width > (len + 2)) ? (width - (len + 2)) : 0; // account for "0x"
        char padch = zero_pad ? '0' : ' ';
        for (int i = 0; i < pad && out_cnt + 1 < n; i++) out[out_cnt++] = padch;
        for (int i = 0; buf[i] && out_cnt + 1 < n; i++) out[out_cnt++] = buf[i];
        fmt++;
        continue;
      } else if (*fmt == 'l') {
        fmt++;
        if (*fmt == 'd') {
          long ld = va_arg(ap, long);
          char buf[32];
          long_to_str(ld, buf);
          int len = (int)strlen(buf);
          int sign = (buf[0] == '-');
          if (sign && zero_pad) {
            if (out_cnt + 1 < n) out[out_cnt++] = '-';
            const char *num = buf + 1;
            int num_len = len - 1;
            int pad = (width > len) ? (width - len) : 0;
            for (int i = 0; i < pad && out_cnt + 1 < n; i++) out[out_cnt++] = '0';
            for (int i = 0; i < num_len && out_cnt + 1 < n; i++) out[out_cnt++] = num[i];
          } else {
            int pad = (width > len) ? (width - len) : 0;
            char padch = zero_pad ? '0' : ' ';
            for (int i = 0; i < pad && out_cnt + 1 < n; i++) out[out_cnt++] = padch;
            for (int i = 0; i < len && out_cnt + 1 < n; i++) out[out_cnt++] = buf[i];
          }
          fmt++;
          continue;
        } else {
          // unsupported %l? treat literally
          out[out_cnt++] = 'l';
          if (*fmt && out_cnt + 1 < n) out[out_cnt++] = *fmt;
          if (*fmt) fmt++;
          continue;
        }
      } else if (*fmt) {
        out[out_cnt++] = *fmt;
        fmt++;
        continue;
      }
    } else {
      out[out_cnt++] = *fmt;
      fmt++;
    }
  }
  out[out_cnt] = '\0';
  return out_cnt;
}



#endif
