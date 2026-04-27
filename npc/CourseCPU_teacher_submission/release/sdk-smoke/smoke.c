#include <am.h>

static int fib(int n) {
  int a = 0, b = 1;
  for (int i = 0; i < n; i++) {
    int c = a + b;
    a = b;
    b = c;
  }
  return a;
}

int main(const char *args) {
  (void)args;
  putch('O');
  putch('K');
  putch('\n');
  return fib(10) == 55 ? 0 : 1;
}
