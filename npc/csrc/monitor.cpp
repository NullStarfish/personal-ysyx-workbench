#include "monitor.h"

#include <cstdio>
#include <cstdlib>
#include <getopt.h>

#include "cpu.h"
#include "difftest_runtime.h"
#include "log/log.h"
#include "mem.h"
#include "runtime.h"
#include "sdb/sdb.h"
#include "sim.h"

#ifdef CONFIG_FTRACE
#include "trace/ftrace.h"
#endif

#ifdef CONFIG_ITRACE
#include "tools/disasm.h"
#endif

char *img_file = nullptr;

namespace {
char *logFile = nullptr;
char *elfFile = nullptr;
char *diffSoFile = nullptr;

long load_program(const char *filename) {
  if (filename == nullptr) {
    return 0;
  }
  FILE *fp = fopen(filename, "rb");
  if (fp == nullptr) {
    printf("Can not open '%s'\n", filename);
    exit(1);
  }
  fseek(fp, 0, SEEK_END);
  long fileSize = ftell(fp);
  fseek(fp, 0, SEEK_SET);
  printf("The image is %s, size = %ld\n", filename, fileSize);
  auto *programData = static_cast<uint8_t *>(malloc(fileSize));
  fread(programData, fileSize, 1, fp);
  mem.loadDataToRom(programData, fileSize);
  free(programData);
  fclose(fp);
  return fileSize;
}

void parse_args(int argc, char *argv[]) {
  const option table[] = {
      {"batch", no_argument, nullptr, 'b'},
      {"log", required_argument, nullptr, 'l'},
      {"diff", required_argument, nullptr, 'd'},
      {"ftrace", required_argument, nullptr, 'f'},
      {"help", no_argument, nullptr, 'h'},
      {0, 0, nullptr, 0},
  };

  int opt = 0;
  while ((opt = getopt_long(argc, argv, "-bl:d:f:h", table, nullptr)) != -1) {
    switch (opt) {
      case 'b': sdb_set_batch_mode(); break;
      case 'l': logFile = optarg; break;
      case 'd': diffSoFile = optarg; break;
      case 'f': elfFile = optarg; break;
      case 1:
        if (img_file == nullptr) {
          img_file = optarg;
        }
        break;
      default: exit(0);
    }
  }
  if (img_file == nullptr && optind < argc) {
    img_file = argv[optind];
  }
}

void welcome() {
  printf("Welcome to the RISC-V NPC simulator!\nFor help, type \"help\"\nThe current img is %s\n", img_file);
}
}

void init_monitor(int argc, char *argv[]) {
  parse_args(argc, argv);
  init_log(logFile);

#ifdef CONFIG_FTRACE
  printf("FTRACE is ON\n");
  init_ftrace(elfFile);
#endif

  runtime.initVerilator(argc, argv);

#ifdef CONFIG_ITRACE
  printf("Disassembler for NPC log: ON\n");
  init_disasm();
#endif

  runtime.setDpiScope();
  long imageSize = load_program(img_file);
  runtime.syncAfterLoad();
  runtime.resetCpu(100);
  cpu.init();
  printf("CPU reset complete.\n");

#ifdef CONFIG_DIFFTEST
  difftest.init(diffSoFile, imageSize);
#endif

  init_sdb();
  welcome();
}
