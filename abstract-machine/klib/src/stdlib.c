#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)
static unsigned long int next = 1;

typedef union header {
    struct {
        union header *next; // 指向下一个空闲块
        size_t size;        // 当前块的大小 (包括头部)   (注意：32位是32位机)
    } s;
    long long align_dummy; // 用于保证头部按 8 字节对齐
} Header;

static Header base;          // 空闲链表的头节点 (哨兵节点)
static Header *freep = NULL; // 指向空闲链表的起始位置



int rand(void) {
  // RAND_MAX assumed to be 32767
  next = next * 1103515245 + 12345;
  return (unsigned int)(next/65536) % 32768;
}

void srand(unsigned int seed) {
  next = seed;
}

int abs(int x) {
  return (x < 0 ? -x : x);
}

int atoi(const char* nptr) {
  int x = 0;
  while (*nptr == ' ') { nptr ++; }
  while (*nptr >= '0' && *nptr <= '9') {
    x = x * 10 + *nptr - '0';
    nptr ++;
  }
  return x;
}

void *malloc(size_t size) {
  // On native, malloc() will be called during initializaion of C runtime.
  // Therefore do not call panic() here, else it will yield a dead recursion:
  //   panic() -> putchar() -> (glibc) -> malloc() -> panic()
//#if !(defined(__ISA_NATIVE__) && defined(__NATIVE_USE_KLIB__))

  if (size == 0) return NULL; // 1. size 为 0 时直接返回 NULL

  size_t nunits = (size + sizeof(Header) - 1) / sizeof(Header) + 1;
  if (freep == NULL) {
        base.s.next = &base;
        base.s.size = 0;
        freep = &base;
        // 在 NEMU AM 中，整个堆区一开始是一个大空闲块
        // 需要将 heap.start 到 heap.end 这块内存 "free" 进来
        // 假设 heap 是 AM 提供的描述堆区的结构体
        Header *p = (Header *)heap.start;
        p->s.size = (heap.end - heap.start) / sizeof(Header);
        free((void *)(p + 1)); // 将整个堆加入空闲链表
    }

    Header *prevp = freep;
    Header *p = prevp->s.next;

    // 2. 遍历空闲链表寻找合适的块
    for (; ; prevp = p, p = p->s.next) {
        if (p->s.size >= nunits) { // 找到了足够大的块
            if (p->s.size == nunits) { // 大小刚刚好
                prevp->s.next = p->s.next;
            } else { // 块太大，需要分割
                p->s.size -= nunits;
                p += p->s.size; // 移动到新块的头部位置
                p->s.size = nunits;
            }
            freep = prevp; // 记录下次查找的起始点
            return (void *)(p + 1); // 5. 返回有效载荷的指针
        }
        if (p == freep) { // 遍历一圈回到起点，说明没有可用内存
            return NULL; // 4. 找不到合适的块
        }
    }
//#endif

  //return NULL;
}

void free(void *ptr) {
  if (ptr == NULL) {
        return;
    }

    // 1. 获取块头部
    Header *bp = (Header *)ptr - 1;

    Header *p = freep;
    // 2. 寻找插入位置 (保持链表按地址有序)
    for (; !(bp > p && bp < p->s.next); p = p->s.next) {
        // 如果 p >= p->s.next, 说明链表中只有 p 一个块或链表是环形的
        if (p >= p->s.next && (bp > p || bp < p->s.next)) {
            break;
        }
    }

    // 3. 尝试与后面的块合并
    if (bp + bp->s.size == p->s.next) {
        bp->s.size += p->s.next->s.size;
        bp->s.next = p->s.next->s.next;
    } else {
        bp->s.next = p->s.next;
    }

    // 3. 尝试与前面的块合并
    if (p + p->s.size == bp) {
        p->s.size += bp->s.size;
        p->s.next = bp->s.next;
    } else {
        p->s.next = bp;
    }

    freep = p; // 更新链表指针
}

#endif
