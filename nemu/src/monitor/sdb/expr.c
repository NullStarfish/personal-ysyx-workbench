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

#include <isa.h>

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>
#include <stdbool.h>
#include <stdint.h>

enum {
  TK_NOTYPE = 256, TK_EQ,
  TK_NUMBER, TK_HEX, TK_REG,
  /* TODO: Add more token types */

};

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   */

  {" +", TK_NOTYPE},    // spaces
  {"\\+", '+'},         // plus
  {"==", TK_EQ},        // equal
  {"-", '-'},         // minus
  {"\\*", '*'},         // multiply
  {"/", '/'},         // divide
  {"\\(", '('},         // left parenthesis
  {"\\)", ')'},         // right parenthesis
  {"[0-9]+", TK_NUMBER},      // number
  {"[a-zA-Z_][a-zA-Z0-9_]*", '1'}, // identifier
  {"0x[0-9a-fA-F]+", '2'}, // hex number
  {"\\$(\\$0|ra|[sgt]p|t[0-6]|a[0-7]|s([0-9]|1[0-1]))", TK_REG},//reg
  {"0[xX][0-9a-fA-F]+",TK_HEX},    //hex

};

#define NR_REGEX ARRLEN(rules) //规则的数量

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
 //这些规则会在简易调试器初始化的时候通过init_regex()被编译成一些用于进行pattern匹配的内部信息, 这些内部信息是被库函数使用的
//通过这个函数，规则数组中的每个正则表达式都会被编译成内部表示形式，存储在 re 数组的相应位置，供后续使用。

//re用来记录编译后的正则表达式信息
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

// tokens数组用于按顺序存放已经被识别出的token信息,
// nr_token指示已经被识别出的token数目.
static Token tokens[32] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;


/*它用position变量来指示当前处理到的位置, 并且按顺序尝试用不同的规则来匹
配当前位置的字符串. 当一条规则匹配成功, 并且匹配出的子串正好是position所在位置的时候, 我
就成功地识别出一个token, Log()宏会输出识别成功的信息. 你需要做的是将识
别出的token信息记录下来(一个例外是空格串), 我们使用Token结构体来记录token的信息:*/
static bool make_token(char *e) {
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i ++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */
        
        switch (rules[i].token_type) {
          case TK_NOTYPE: break; // Ignore this token
          case TK_EQ: case '+': case '-': case '*': case '/':
          case '(': case ')': case TK_NUMBER: case TK_HEX:
          case TK_REG: case '1': case '2':
            // Handle other token types as needed
            strncpy(tokens[nr_token].str, substr_start, substr_len);
            tokens[nr_token].str[substr_len] = '\0';
            tokens[nr_token].type = rules[i].token_type;
            nr_token++;
            break;
          default: break;
        }

        break;
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }

  return true;
}


bool check_parentheses(int p, int q, bool* badexpr) {

  if (tokens[p].type == '(' && tokens[q].type == ')') {
    bool result = true;
    int parentheses_cnt_stack = 1;
    for (int i = p + 1; i <= q - 1; i ++) {
      if (tokens[i].type == '(') {
        parentheses_cnt_stack ++;
      } else if (tokens[i].type == ')') {
        parentheses_cnt_stack --;
      }
      if (parentheses_cnt_stack == 0) {
        result = false;
      }
      if (parentheses_cnt_stack < 0) {
        printf("err in check_parenttheses : bad expr\n");
        *badexpr = true; result = false;
      }
    }
    return result;
  }
  return false;
}
uint32_t eval(int p, int q, bool* badexpr) {
  if (p > q) {
    /* Bad expression */
    printf("[debug] err in eval p > q\n");
    *badexpr = true;
    return 1;
  }
  else if (p == q) {
    /* Single token.
     * For now this token should be a number.
     * Return the value of the number.
     */
     if (tokens[p].type != TK_NUMBER) {
      printf("err in p == q not a number \n");
      *badexpr = true;
      return 1;
     }
     printf("success return p == q number %d\n", atoi(tokens[p].str));
     return atoi(tokens[p].str);
  }
  else if (check_parentheses(p, q, badexpr) == true) {
    /* The expression is surrounded by a matched pair of parentheses.
     * If that is the case, just throw away the parentheses.
     */
    printf("success take off the parenttheses\n");
    return eval(p + 1, q - 1, badexpr);
  }
  else {
    if (*badexpr) {
      printf("someting happen in sub check\n");
      return 1;
    }
    //op = the position of 主运算符 in the token expression;
    int op = p;
    int in_parentheses = 0;
    int first_mult_div = true;
    for (int i = p; i <= q; i ++) {
      printf("tokens[i] %s\n", tokens[i].str);
      printf("tokens[op] %s\n", tokens[op].str);
      printf("in_parent %d\n", in_parentheses);
      printf("token[i] type: %c\n", tokens[i].type);
      switch (tokens[i].type) {

        case '+': case'-':
          printf("match + or -!\n");
          if (in_parentheses == 0)
            op = i; 
          break;

        case '(':
          in_parentheses++;
          break;

        case ')':
          in_parentheses--;
          break;

        case '*': case '/':
          if (first_mult_div) {
            op = i;
            first_mult_div = false;
          }
          break;
        
        default:
          printf("didn't match anything \n");
          break;
      }
      if (tokens[op].type == '+' || tokens[op].type == '-')
        break;

    }
    printf("find domain op %s\n", tokens[op].str);
    bool badexpr1 = false, badexpr2 = false;
    uint32_t val1 = eval(p, op - 1, &badexpr1);
    uint32_t val2 = eval(op + 1, q, &badexpr2);
    *badexpr = badexpr1 || badexpr2;
    if (*badexpr) {
      printf("err in sub eval\n");
      return 1;
    }
    switch (tokens[op].type) {
      case '+': return val1 + val2;
      case '-': return val1 - val2;
      case '*': return val1 * val2;
      case '/': return val1 / val2;
      default: *badexpr = true; return 1;
    }
  }
}

word_t expr(char *e, bool *success) {
  if (!make_token(e)) {
    *success = false;
    return 0;
  }

  /* TODO: Insert codes to evaluate the expression. */
  bool badexpr = false;
  uint32_t ret = eval(0, nr_token - 1, &badexpr);
  *success = !badexpr;
  if (*success)
    printf("%u\n", ret);
  return 0;
}
