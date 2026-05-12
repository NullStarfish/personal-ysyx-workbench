volatile int a[32][32];
volatile int b[32][32];
int c[32][32];
#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdlib.h>

int main() {
   for (int i = 0; i < 32; i ++) {
        for (int j = 0; j < 32; j ++) {
           a[i][j] = (rand() >> 12) - 0x500;
           b[i][j] = (rand() >> 12) - 0x500;
        }
   } 


   for (int i = 0; i <32; i ++) {
    for (int j = 0; j < 32; j ++) {
        for (int k = 0; k < 32; k ++) {
            c[i][j] += a[i][k] * b[k][j];
        }
    }
   }

   for (int i = 0; i < 32; i ++) {
    for (int j = 0; j < 32; j ++) {
        printf("%d  ", c[i][j]);
    }
    printf("\n");
   }

}