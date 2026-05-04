#include <am.h>
#include <klib.h>
#include <klib-macros.h>
int main(){
    ioe_init();
    while(1) {
        int rx = (unsigned char)io_read(AM_UART_RX).data;
        if (rx != 0xff)
            printf("%c\n", rx);
    }
}