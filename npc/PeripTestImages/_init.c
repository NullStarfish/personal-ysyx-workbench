
int main();

void _init() {
    int ret = main();
    __asm__ volatile("mv a0, %[ret]\n\t"
        "ebreak"
        :
        : [ret] "r"(ret)
        : "a0", "memory");
}