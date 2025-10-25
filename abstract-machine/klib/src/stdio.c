#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>
#include <string.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

// 健壮的辅助函数：将整数转为字符串
static int int_to_str(int n, char *buf) {
  char *p = buf;
  if (n == 0) {
    *p++ = '0';
    *p = '\0';
    return 1;
  }
  
  // 处理INT_MIN的特殊情况，因为它不能安全地取反
  unsigned int u;
  if (n == -2147483648) { // INT_MIN for 32-bit
    strcpy(buf, "-2147483648");
    return 11;
  }

  if (n < 0) {
    *p++ = '-';
    u = -n;
  } else {
    u = n;
  }

  char tmp[20];
  int i = 0;
  while (u > 0) {
    tmp[i++] = (u % 10) + '0';
    u /= 10;
  }

  // 将tmp中的数字逆序拷贝到buf中
  while (i-- > 0) {
    *p++ = tmp[i];
  }
  *p = '\0';

  return p - buf;
}

// 健壮的辅助函数：将无符号整数转为十六进制字符串
static int uint_to_hex(unsigned int n, char *buf) {
    if (n == 0) {
        buf[0] = '0';
        buf[1] = '\0';
        return 1;
    }
    char hex_chars[] = "0123456789abcdef";
    char tmp[10];
    int i = 0;
    while (n > 0) {
        tmp[i++] = hex_chars[n & 0xF];
        n >>= 4;
    }
    int len = i;
    int j = 0;
    while (i-- > 0) {
        buf[j++] = tmp[i];
    }
    buf[j] = '\0';
    return len;
}


int printf(const char *fmt, ...) {
  char buf[2048]; // 增大缓冲区以防万一
  va_list args;
  va_start(args, fmt);
  int len = vsnprintf(buf, sizeof(buf), fmt, args);
  va_end(args);
  for(int i = 0; i < len; i++) {
    putch(buf[i]);
  }
  return len;
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  return vsnprintf(out, (size_t)-1, fmt, ap);
}

int sprintf(char *out, const char *fmt, ...) {
  va_list args;
  va_start(args, fmt);
  int ret = vsprintf(out, fmt, args);
  va_end(args);
  return ret;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    int ret = vsnprintf(out, n, fmt, args);
    va_end(args);
    return ret;
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  char *str = out;
  char *end = out + n -1;
  
  while (*fmt) {
    if (str >= end) break;

    if (*fmt == '%') {
      fmt++;
      if (*fmt == '\0') break;

      if (*fmt == 's') {
        const char *s = va_arg(ap, const char*);
        if (s == NULL) s = "(null)";
        while (*s && str < end) {
          *str++ = *s++;
        }
      } else if (*fmt == 'd') {
        int d = va_arg(ap, int);
        char num_buf[20];
        int len = int_to_str(d, num_buf);
        for (int i = 0; i < len && str < end; i++) {
          *str++ = num_buf[i];
        }
      } else if (*fmt == 'u') {
        // 简单实现无符号，可以基于有符号修改
        unsigned int u = va_arg(ap, unsigned int);
        if (u == 0) {
             if (str < end) *str++ = '0';
        } else {
            char num_buf[20];
            int i = 0;
            while(u > 0){
                num_buf[i++] = (u % 10) + '0';
                u /= 10;
            }
            while(i-- > 0 && str < end){
                *str++ = num_buf[i];
            }
        }
      } else if (*fmt == 'x' || *fmt == 'p') {
        unsigned int p = va_arg(ap, unsigned int);
        char hex_buf[20];
        if (*fmt == 'p' && str + 2 < end) {
            *str++ = '0';
            *str++ = 'x';
        }
        int len = uint_to_hex(p, hex_buf);
        for (int i = 0; i < len && str < end; i++) {
          *str++ = hex_buf[i];
        }
      } else if (*fmt == 'c') {
        char c = (char)va_arg(ap, int);
        if (str < end) *str++ = c;
      } else if (*fmt == '%') {
        if (str < end) *str++ = '%';
      } else {
        // 不支持的格式，原样输出
        if (str < end) *str++ = '%';
        if (str < end) *str++ = *fmt;
      }
    } else {
      *str++ = *fmt;
    }
    fmt++;
  }
  
  *str = '\0';
  return str - out;
}

#endif