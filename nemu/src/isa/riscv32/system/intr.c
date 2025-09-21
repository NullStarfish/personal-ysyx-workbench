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

word_t isa_raise_intr(word_t NO, vaddr_t epc) {
  // 保存PC和异常原因
  cpu.csrs.mepc = epc;
  cpu.csrs.mcause = NO;

  // --- 更新 mstatus 寄存器 ---
  // 1. 将 mstatus.MIE (bit 3) 的当前值保存到 mstatus.MPIE (bit 7)
  //    然后将 mstatus.MIE 设置为 0 来禁用中断
  if ((cpu.csrs.mstatus >> 3) & 1) { // if MIE == 1
    cpu.csrs.mstatus |= (1 << 7);    // set MPIE = 1
  } else {
    cpu.csrs.mstatus &= ~(1 << 7);   // set MPIE = 0
  }
  cpu.csrs.mstatus &= ~(1 << 3);     // set MIE = 0

  // 2. 在 mstatus.MPP (bits 11-12) 中保存当前的特权级
  //    注意: 这里我们假设CPU总是在M-mode下运行，所以硬编码为0b11。
  //    当你的NEMU完整支持多级特权后，这里应该读取CPU的当前特权级。
  //    在你修复mret之前，这个假设是成立的。修复后，当CPU在U-mode时，
  //    这里应该写入0b00，这需要你的CPU状态机支持特权级跟踪。
  //    但对于通过am-test来说，仅修复mret就足够了。
  word_t current_privilege = 3; // 假设当前是M-mode (0b11)
  cpu.csrs.mstatus &= ~((1 << 11) | (1 << 12)); // 清空 MPP 字段
  cpu.csrs.mstatus |= (current_privilege << 11); // 设置为当前权限

  // 返回异常向量表的地址
  return cpu.csrs.mtvec;
}

word_t isa_query_intr() {
  return INTR_EMPTY;
}
