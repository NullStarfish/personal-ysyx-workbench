#include <klib.h>
#include <klib-macros.h>
#include <stddef.h>
#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char *s) {
  for (int cnt = 0; ; cnt++) {
    if (s[cnt] == '\0') return cnt;
  }
}



char *strcpy(char *dst, const char *src) {
  memcpy(dst, src, strlen(src) + 1); // 包括结尾的'\0'
  return dst;
}

char *strncpy(char *dst, const char *src, size_t n) {
  memcpy(dst, src, n);
  if (n > 0) {
    dst[n - 1] = '\0'; // 确保字符串以'\0'结尾
  }
  return dst;
}

char *strcat(char *dst, const char *src) {
  int len = strlen(dst);
  for (int i = 0; ; i++) {
    dst[len + i] = src[i];
    if (src[i] == '\0') return dst;
  }
}

int strcmp(const char *s1, const char *s2) {
  for (int i = 0; ; i++) {
    if (s1[i] != s2[i]) return (unsigned char)s1[i] - (unsigned char)s2[i];
    if (s1[i] == '\0' && s2[i] == '\0') return 0; // both strings ended
  }
}

int strncmp(const char *s1, const char *s2, size_t n) {
  for (int i = 0; i < n; i++) {
    if (s1[i] != s2[i]) return (unsigned char)s1[i] - (unsigned char)s2[i];
    if (s1[i] == '\0' && s2[i] == '\0') return 0; // both strings ended
  }
  return 0;
}

void *memset(void *s, int c, size_t n) {
  unsigned char *p = s;
  for (size_t i = 0; i < n; i++) {
    p[i] = (unsigned char)c;
  }
  return s;
}


/**
      The  memmove()  function  copies n bytes from memory area src to memory area dest.  The memory areas may overlap: copying takes place as though
      the bytes in src are first copied into a temporary array that does not overlap src or dest, and the bytes are then copied  from  the  temporary
      array to dest.
 */


void *memmove(void *dst, const void *src, size_t n) {
  unsigned char temp[n];
  const unsigned char *p_src = src;
  unsigned char *p_dst = dst;
  // Copy from src to temp
  for (size_t i = 0; i < n; i++) {
    temp[i] = p_src[i];
  }
  // Copy from temp to dst
  for (size_t i = 0; i < n; i++) {
    p_dst[i] = temp[i];
  }
  return dst;
  
}

void *memcpy(void *out, const void *in, size_t n) {
  unsigned char *p_out = out;
  const unsigned char *p_in = in;
  for (size_t i = 0; i < n; i++) {
    p_out[i] = p_in[i];
  }
  return out;
}

void *mempcpy(void *out, const void *in, size_t n) {
  unsigned char *p_out = out;
  const unsigned char *p_in = in;
  for (size_t i = 0; i < n; i++) {
    p_out[i] = p_in[i];
  }
  return p_out + n; // 返回指向复制后位置的指针
}

int memcmp(const void *s1, const void *s2, size_t n) {
  const unsigned char *p1 = s1;
  const unsigned char *p2 = s2;
  for (size_t i = 0; i < n; i++) {
    if (p1[i] != p2[i]) {
      return p1[i] - p2[i];
    }
  }
  return 0; // equal
}

#endif
