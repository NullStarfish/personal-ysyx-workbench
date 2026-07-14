#ifndef NPC_RUNTIME_PROGRAM_IMAGE_H
#define NPC_RUNTIME_PROGRAM_IMAGE_H

#include <cstddef>
#include <string>

class Memory;

class ProgramImage {
public:
  explicit ProgramImage(std::string path = {});

  void init(Memory &memory);
  const char *path() const;
  size_t size() const;

private:
  std::string pathValue;
  size_t sizeValue = 0;
};

#endif
