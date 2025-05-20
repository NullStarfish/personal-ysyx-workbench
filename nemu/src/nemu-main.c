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

#include <common.h>


//#define EXPR_TEST

void init_monitor(int, char *[]);
void am_init_monitor();
void engine_start();
int is_exit_status_bad();

word_t expr(char *e, bool *success);

int main(int argc, char *argv[]) {
  /* Initialize the monitor. */
#ifdef CONFIG_TARGET_AM
  am_init_monitor();
#else
  init_monitor(argc, argv);
#endif

  /* Start engine. */
  #ifndef EXPR_TEST
  engine_start();
  #endif


  #ifdef EXPR_TEST
  char buf[65536 + 10];

  FILE *fp = NULL;
  fp = fopen("/home/nullstarfish/Desktop/ysyx-workbench/nemu/input", "r");
  assert(fp != NULL);
  printf("start testing\n");
  while (fgets(buf, 65535 + 10, fp) != NULL) {
    int result;
    char *expression = NULL;
    // 找到第一个空格后面的内容
    char *space = strchr(buf, ' ');
    if (space == NULL) continue; // 格式不对
    *space = '\0';
    result = atoi(buf);
    expression = space + 1;
    // 去掉结尾换行
    char *newline = strchr(expression, '\n');
    if (newline) *newline = '\0';

    

    bool success = true;
    int process = expr(expression, &success);
    if (process == result)
      printf("test passed %u = %s\n", result, expression);
    else 
      printf("failed. expect %s = %u, but returned %u\n", expression, result, process);
  }
  #endif
  

#ifndef EXPR_TEST

  return is_exit_status_bad();
#endif

}

//make run | grep fail