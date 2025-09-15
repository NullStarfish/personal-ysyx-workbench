#include <am.h>
#include <nemu.h>

#define KEYDOWN_MASK 0x8000

void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  // 1. 从设备读取原始键盘事件
  uint32_t ev = inl(KBD_ADDR);

  // 2. 使用位运算直接解码，无需分支判断
  
  // (ev & KEYDOWN_MASK) 的结果要么是 KEYDOWN_MASK(非零)，要么是 0。
  // 在 C 语言中，非零值在布尔上下文中为 true (1)，零为 false (0)。
  // 这行代码直接将按键状态 (第15位) 提取并赋值给 keydown。
  kbd->keydown = (ev & KEYDOWN_MASK) != 0;

  // 使用位与和取反操作，无论原始 ev 的第15位是0还是1，
  // 最终都能得到清零该位后的结果，即实际的键码。
  kbd->keycode = ev & ~KEYDOWN_MASK;
}

//这个函数不仅仅实现了胶水，还进行了抽象：把断码进行解析，看是哪个键释放了