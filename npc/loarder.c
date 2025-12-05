#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>

// 假设你的物理内存从 0x80000000 开始 (和你的链接脚本 -Ttext 一致)
#define MEM_BASE_ADDR 0x80000000
// 假设分配 1MB 的内存空间给仿真器
#define MEM_SIZE (1024 * 1024) 

int main() {
    const char *filename = "char-test.bin";
    FILE *fp = fopen(filename, "rb");
    if (!fp) {
        perror("无法打开文件");
        return 1;
    }

    // 1. 获取文件大小
    fseek(fp, 0, SEEK_END);
    long filesize = ftell(fp);
    rewind(fp);

    printf("文件大小: %ld 字节\n", filesize);

    // 2. 准备模拟内存 (使用 uint8_t 数组)
    uint8_t *ram = (uint8_t *)malloc(MEM_SIZE);
    if (!ram) {
        printf("内存分配失败\n");
        fclose(fp);
        return 1;
    }
    
    // 初始化内存为 0
    // (通常不需要，但为了仿真严谨性建议做)
    for(int i=0; i<MEM_SIZE; i++) ram[i] = 0;

    // 3. 将文件加载到内存中
    // 注意：fread 读取的是字节流，这模拟了 DMA 或 Bootloader 将代码搬运到 RAM 的过程
    size_t result = fread(ram, 1, filesize, fp);
    if (result != filesize) {
        printf("读取错误\n");
    }
    fclose(fp);

    printf("代码加载完成。开始模拟取指 (Fetch)...\n\n");

    // 4. 模拟取指 (Instruction Fetch)
    // RISC-V 32位指令是4字节对齐的
    for (int pc_offset = 0; pc_offset < filesize; pc_offset += 4) {
        
        // 这里的逻辑非常关键：
        // 即使宿主机(你的PC)是大端序，这样写也能保证正确解析小端序的 RISC-V 指令
        uint32_t inst = 
            (uint32_t)ram[pc_offset + 0] |
            (uint32_t)ram[pc_offset + 1] << 8  |
            (uint32_t)ram[pc_offset + 2] << 16 |
            (uint32_t)ram[pc_offset + 3] << 24;

        // 打印结果：模拟 PC 地址 | 原始十六进制指令
        printf("PC [0x%08X] : 指令机器码 = 0x%08X\n", 
               MEM_BASE_ADDR + pc_offset, 
               inst);
               
        // 可以在这里调用你的解码函数，例如：
        // decode_and_execute(inst);
    }

    free(ram);
    return 0;
}