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

//#define DEBUG_EXPR


#ifdef DEBUG_EXPR
#define DEBUG_PRINT(fmt, ...) printf(fmt, ##__VA_ARGS__)
#else
#define DEBUG_PRINT(fmt, ...) 
#endif


word_t vaddr_read(vaddr_t addr, int len);




enum {
  TK_NOTYPE = 256, TK_EQ,
  TK_NUMBER, TK_HEX, TK_REG,
  /* TODO: Add more token types */
  TK_NEG, TK_POS, TK_DEREF, TK_NEQ, TK_LE, TK_GE, TK_LT, TK_GT,
  TK_AND, TK_OR, TK_NOT
};

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   */

  {" +", TK_NOTYPE},    // spaces

  {"==", TK_EQ},        // equal
  {"!=", TK_NEQ},       // not equal
  {"<=", TK_LE},        // less than or equal
  {">=", TK_GE},        // greater than or equal
  {"<", TK_LT},         // less than
  {">", TK_GT},         // greater than
  {"&&", TK_AND},       // and
  {"\\|\\|", TK_OR},    // or
  {"!", TK_NOT},        // not
  {"\\+", '+'},         // plus
  {"-", '-'},         // minus
  {"\\*", '*'},         // multiply
  {"/", '/'},         // divide
  {"\\(", '('},         // left parenthesis
  {"\\)", ')'},         // right parenthesis
  {"0[xX][0-9a-fA-F]+",TK_HEX},    //hex must be placed before number
  {"[0-9]+", TK_NUMBER},      // number
  {"\\$(\\$0|ra|pc|[sgt]p|t[0-6]|a[0-7]|s([0-9]|1[0-1]))", TK_REG},//reg
  

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
//init_regex之后，仅仅对正则表达式进行了识别，但是并没有进行计算规则的定义

typedef struct token {
  int type;
  char str[32];
} Token;


// tokens数组用于按顺序存放已经被识别出的token信息,
// nr_token指示已经被识别出的token数目.
//static Token tokens[32] __attribute__((used)) = {};
static Token tokens[65536] __attribute__((used)) = {};//use to test the expr
static int nr_token __attribute__((used))  = 0;

static struct Op_priority {
  int type;
  int priority;
} priority[] = {
  {TK_EQ, 7},
  {TK_NEQ, 7},
  {TK_AND, 11},
  {TK_OR, 12},
  {TK_NOT, 2},
  {TK_LE, 6},
  {TK_GE, 6},
  {TK_LT, 6},
  {TK_GT, 6},
  {'+', 4},
  {'-', 4},
  {'*', 3},
  {'/', 3},
  {TK_POS, 2},
  {TK_NEG, 2},
  {TK_DEREF, 2}
};

#define NR_PRIORITY ARRLEN(priority)

int get_priority(int token_type) {
  for (int i = 0; i < NR_PRIORITY; i ++) {
    if (priority[i].type == token_type) {
      return priority[i].priority;
    }
  }
  return -1;
}



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
          default:
            // Handle other token types as needed
            strncpy(tokens[nr_token].str, substr_start, substr_len);
            tokens[nr_token].str[substr_len] = '\0';
            tokens[nr_token].type = rules[i].token_type;
            nr_token++;
            break;
        }
        break;
      }
    }

    if (i == NR_REGEX) {
      DEBUG_PRINT("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
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
        DEBUG_PRINT("err in check_parenttheses : bad expr\n");
        *badexpr = true; result = false;
      }
    }
    return result;
  }
  return false;
}

int pre_token_process() {
  for (int i = 0; i < nr_token; i ++) {
    if (tokens[i].type == '+' || 
        tokens[i].type == '-' ||
        tokens[i].type == '*') {
      if (i == 0 || (tokens[i - 1].type != TK_NUMBER && tokens[i - 1].type != TK_HEX && tokens[i - 1].type != ')')) {//判断逻辑
        switch (tokens[i].type) {
          case '+': tokens[i].type = TK_POS; break;
          case '-': tokens[i].type = TK_NEG; break;
          case '*': tokens[i].type = TK_DEREF; break;
          default: break;
        }
      }
    } 
  }
  return 0;
}




int find_major(int p, int q) {
  int op = -1;
  int max_priority = -1; // 设个大的初始值
  int parentheses = 0; //括号数量
  for (int i = p; i <= q; i ++) {
    DEBUG_PRINT("tokens[i] %s\n", tokens[i].str);
    DEBUG_PRINT("tokens[op] %s\n", tokens[op].str);
    DEBUG_PRINT("token[i] type: %d\n", tokens[i].type);
    if (tokens[i].type == '(') {
        parentheses++;
        continue;
    }
    if (tokens[i].type == ')') {
        parentheses--;
        continue;
    }
    if (parentheses != 0)
        continue;
  
    int priority = get_priority(tokens[i].type);

    
    DEBUG_PRINT("priority: %d\n", priority);

    // 如果当前运算符的序号大于或等于之前找到的，则更新 op
    //priority越大，越后算
    if (priority >= 0 && priority >= max_priority) {
        max_priority = priority;
        op = i;
    }
    DEBUG_PRINT("op: %d\n", op);
  }

  return op;
}


uint32_t eval(int p, int q, bool* badexpr) {
  if (p > q) {
    /* Bad expression */
    DEBUG_PRINT("[debug] err in eval p > q\n");
    *badexpr = true;
    return 0;
  }
  else if (p == q) {
    /* Single token.
     * For now this token should be a number.
     * Return the value of the number.
     */
     //DEBUG_PRINT("success return p == q number %d\n", atoi(tokens[p].str));
     bool success = false;
     if (tokens[p].type == TK_REG) {
      return isa_reg_str2val(tokens[p].str + 1, &success);
     } else if (tokens[p].type == TK_HEX) {
      return strtol(tokens[p].str, NULL, 16);
     }
     else if (tokens[p].type == TK_NUMBER) {
      //DEBUG_PRINT("success return p == q number %d\n", atoi(tokens[p].str));
      return atoi(tokens[p].str);
     } else {
        DEBUG_PRINT("err in p == q not a number \n");
        *badexpr = true;
        return 0;
      }
  }
  else if (check_parentheses(p, q, badexpr) == true) {
    /* The expression is surrounded by a matched pair of parentheses.
     * If that is the case, just throw away the parentheses.
     */
    DEBUG_PRINT("success take off the parenttheses\n");
    return eval(p + 1, q - 1, badexpr);
  }
  else {
    if (*badexpr) {
      DEBUG_PRINT("someting happen in sub parenthese check\n");
      return 0;
    }
    //op = the position of 主运算符 in the token expression;
    int op = find_major(p, q);
    if (op == -1) {
      *badexpr = true;
      return 0;
    }
    DEBUG_PRINT("find domain op %s\n", tokens[op].str);


    bool badexpr1 = false, badexpr2 = false;
    uint32_t val2 = eval(op + 1, q, &badexpr2);
    if (tokens[op].type == TK_NEG) 
      return -val2;
    else if (tokens[op].type == TK_POS) 
      return val2;
    else if (tokens[op].type == TK_DEREF) {
      // deref
      return vaddr_read(val2, 4);
    } else if (tokens[op].type == TK_NOT) {
      return !val2;
    }
    




    uint32_t val1 = eval(p, op - 1, &badexpr1);
    *badexpr = badexpr1 || badexpr2;
    if (*badexpr) {
      DEBUG_PRINT("err in sub eval\n");
      return 1;
    }
    switch (tokens[op].type) { //运算符 双目
      case '+': return val1 + val2;
      case '-': return val1 - val2;
      case '*': return val1 * val2;
      case '/': return val1 / val2;
      case TK_EQ: return val1 == val2;
      case TK_NEQ: return val1 != val2;
      case TK_LE: return val1 <= val2;
      case TK_GE: return val1 >= val2;
      case TK_LT: return val1 < val2;
      case TK_GT: return val1 > val2;
      case TK_AND: return val1 && val2;
      case TK_OR: return val1 || val2;
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
  pre_token_process();


  bool badexpr = false;
  uint32_t ret = eval(0, nr_token - 1, &badexpr);
  *success = !badexpr;
  if (*success)
    return ret;
  return 0;
}
