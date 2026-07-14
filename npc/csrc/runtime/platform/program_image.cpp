#include "runtime/platform/program_image.h"

#include <cstdio>
#include <cstdlib>
#include <utility>
#include <vector>

#include "runtime/platform/memory.h"

ProgramImage::ProgramImage(std::string path) : pathValue(std::move(path)) {}

void ProgramImage::init(Memory &memory) {
  if (pathValue.empty()) return;
  FILE *file = fopen(pathValue.c_str(), "rb");
  if (file == nullptr) {
    printf("Can not open '%s'\n", pathValue.c_str());
    exit(1);
  }
  fseek(file, 0, SEEK_END);
  const long fileSize = ftell(file);
  fseek(file, 0, SEEK_SET);
  if (fileSize < 0) {
    fclose(file);
    printf("Can not determine size of '%s'\n", pathValue.c_str());
    exit(1);
  }
  sizeValue = static_cast<size_t>(fileSize);
  std::vector<uint8_t> data(sizeValue);
  if (sizeValue != 0 && fread(data.data(), sizeValue, 1, file) != 1) {
    fclose(file);
    printf("Can not read '%s'\n", pathValue.c_str());
    exit(1);
  }
  fclose(file);
  printf("The image is %s, size = %zu\n", pathValue.c_str(), sizeValue);
  memory.loadDataToRom(data.data(), data.size());
}

const char *ProgramImage::path() const { return pathValue.empty() ? nullptr : pathValue.c_str(); }
size_t ProgramImage::size() const { return sizeValue; }
