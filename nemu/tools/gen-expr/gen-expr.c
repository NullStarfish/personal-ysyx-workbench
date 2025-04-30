/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/


#ifndef DEBUG
#define DEBUG_PRINT(fmt, ...)
#else
#define DEBUG_PRINT(fmt, ...) printf(fmt, ##__VA_ARGS__) 
#endif



#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>

typedef uint32_t word_t;


// this should be enough
static char buf[65536] = {};
static char code_buf[65536 + 128] = {}; // a little larger than `buf`
static char *code_format =
"#include <stdio.h>\n"
"int main() { "
"  unsigned result = %s; "
"  printf(\"%%u\", result); "
"  return 0; "
"}";




static word_t choose(int n) {
  word_t ret = rand() % n;
  DEBUG_PRINT("choose %u\n", ret);
  return ret;
}


static word_t gen(char c) {
  if (strlen(buf) + 2 > sizeof(buf))
    return 0;
  buf[strlen(buf)] = c;
  DEBUG_PRINT("generate %c at %u in %s\n", c, (int)strlen(buf), buf);
  return 1;
}

static word_t gen_num() {
  DEBUG_PRINT("start gen_num\n");
  return gen(choose(10) + '0') && gen('U');
}

static word_t  gen_rand_op() {
  DEBUG_PRINT("start gen_rand_op\n");
  word_t op_index = choose(4);
  switch (op_index) {
    case 0: return gen('+'); 
    case 1: return gen('-'); 
    case 2: return gen('*');
    case 3: return gen('/'); 
    default: break;
  }
  return 0;
}


static word_t gen_rand_expr() {
  word_t mem;
  switch (choose(3)) {
    case 0: 
      mem = strlen(buf);
      if (gen_num()) return 1;
      buf[mem] = '\0';
      return 0;
    case 1: 
      mem = strlen(buf);
      if (gen('('))
        if(gen_rand_expr())
          if(gen(')'))
            return 1;
      buf[mem] = '\0';
      return 0;
    default: 
      mem = strlen(buf);
      if (gen_rand_expr()) if(gen_rand_op()) if(gen_rand_expr()) return 1;
      buf[mem] = '\0';
      return 0;
  }
  buf[strlen(buf)] = '\0';
}

int main(int argc, char *argv[]) {
  int seed = time(0);
  srand(seed);
  int loop = 1;
  if (argc > 1) {
    sscanf(argv[1], "%d", &loop);
  }
  int i;
  for (i = 0; i < loop; i ++) {
    memset(buf, '\0', sizeof(buf));
    gen_rand_expr();

    // 过滤掉 buf 中的所有 'U'
    char expr_noU[65536] = {};
    int j = 0;
    for (int k = 0; buf[k] != '\0'; k++) {
      if (buf[k] != 'U')
        expr_noU[j++] = buf[k];
    }
    expr_noU[j] = '\0';

    sprintf(code_buf, code_format, buf);

    FILE *fp = fopen("/tmp/.code.c", "w");
    assert(fp != NULL);
    fputs(code_buf, fp);
    fclose(fp);

    int ret = system("gcc -Wall -Werror /tmp/.code.c -o /tmp/.expr");
    if (ret != 0) continue;

    fp = popen("/tmp/.expr", "r");
    assert(fp != NULL);

    int result;
    ret = fscanf(fp, "%d", &result);
    pclose(fp);

    printf("%u %s\n", result, expr_noU);
  }
  return 0;
}
