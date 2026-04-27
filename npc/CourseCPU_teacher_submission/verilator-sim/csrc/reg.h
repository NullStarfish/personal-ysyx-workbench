#ifndef __REG_H__
#define __REG_H__

#include <stdint.h>
#include <stdbool.h>

/**
 * @brief Display all register values.
 */
void isa_reg_display();

/**
 * @brief Convert a register name string (e.g., "$pc", "$rax") to its value.
 * * @param s The string containing the register name.
 * @param success A pointer to a boolean that will be set to true on success, false on failure.
 * @return The value of the register.
 */
uint32_t isa_reg_str2val(const char *s, bool *success);

#endif
