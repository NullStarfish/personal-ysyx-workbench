#include <am.h>
#include <klib.h>
#include <klib-macros.h>
int main() {
    ioe_init();
    int sec = 1;
    while (1) {
        while(io_read(AM_TIMER_UPTIME).us / 1000000 < sec) ;
        printf("passed one sec\n");
        sec ++;
    }
}