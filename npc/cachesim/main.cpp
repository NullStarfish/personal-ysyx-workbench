#include "cache_sim.h"

#include <cerrno>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <cctype>
#include <fstream>
#include <iostream>
#include <string>

namespace {

void printUsage(const char *prog) {
  std::fprintf(stderr,
               "Usage: %s [--capacity BYTES] [--ways N] [--tag-bits N] [--index-bits N] [--offset-bits N] [--trace] [addr-file]\n"
               "\n"
               "Address input accepts one request per line. Supported forms include:\n"
               "  0xa0000000\n"
               "  pc 0xa0000000\n"
               "  fetch,0xa0000000\n"
               "Comments start with '#'.\n"
               "If addr-file is omitted, addresses are read from stdin.\n",
               prog);
}

bool parseUnsigned(const char *text, unsigned *out) {
  char *end = nullptr;
  errno = 0;
  unsigned long value = std::strtoul(text, &end, 0);
  if (errno != 0 || end == text || *end != '\0') return false;
  *out = static_cast<unsigned>(value);
  return true;
}

bool parseU32(const char *text, uint32_t *out) {
  char *end = nullptr;
  errno = 0;
  unsigned long value = std::strtoul(text, &end, 0);
  if (errno != 0 || end == text || *end != '\0') return false;
  *out = static_cast<uint32_t>(value);
  return true;
}

bool parseAddrToken(const std::string &text, uint32_t *out) {
  char *end = nullptr;
  errno = 0;
  unsigned long value = std::strtoul(text.c_str(), &end, 0);
  if (errno != 0 || end == text.c_str()) return false;
  while (*end == ' ' || *end == '\t' || *end == '\r' || *end == '\n' || *end == ',') end++;
  if (*end != '\0') return false;
  *out = static_cast<uint32_t>(value);
  return true;
}

bool parseAddr(const std::string &line, uint32_t *out) {
  std::string clean = line;
  const std::size_t comment = clean.find('#');
  if (comment != std::string::npos) {
    clean.resize(comment);
  }

  for (char &ch : clean) {
    if (ch == ',' || ch == ':' || ch == '=') ch = ' ';
  }

  std::size_t pos = 0;
  while (pos < clean.size()) {
    while (pos < clean.size() && std::isspace(static_cast<unsigned char>(clean[pos]))) pos++;
    const std::size_t begin = pos;
    while (pos < clean.size() && !std::isspace(static_cast<unsigned char>(clean[pos]))) pos++;
    if (begin == pos) break;

    uint32_t addr = 0;
    if (parseAddrToken(clean.substr(begin, pos - begin), &addr)) {
      *out = addr;
      return true;
    }
  }
  return false;
}

} // namespace

int main(int argc, char **argv) {
  cachesim::CacheConfig config;
  bool trace = false;
  const char *addrFile = nullptr;

  for (int i = 1; i < argc; i++) {
    const char *arg = argv[i];
    auto requireValue = [&](const char *name) -> const char * {
      if (i + 1 >= argc) {
        std::fprintf(stderr, "%s requires a value\n", name);
        std::exit(1);
      }
      return argv[++i];
    };

    if (std::strcmp(arg, "--tag-bits") == 0) {
      if (!parseUnsigned(requireValue(arg), &config.tagBits)) {
        std::fprintf(stderr, "invalid --tag-bits value\n");
        return 1;
      }
    } else if (std::strcmp(arg, "--index-bits") == 0) {
      if (!parseUnsigned(requireValue(arg), &config.indexBits)) {
        std::fprintf(stderr, "invalid --index-bits value\n");
        return 1;
      }
    } else if (std::strcmp(arg, "--offset-bits") == 0) {
      if (!parseUnsigned(requireValue(arg), &config.offsetBits)) {
        std::fprintf(stderr, "invalid --offset-bits value\n");
        return 1;
      }
    } else if (std::strcmp(arg, "--ways") == 0) {
      if (!parseUnsigned(requireValue(arg), &config.ways)) {
        std::fprintf(stderr, "invalid --ways value\n");
        return 1;
      }
    } else if (std::strcmp(arg, "--capacity") == 0 || std::strcmp(arg, "--capacity-bytes") == 0) {
      if (!parseU32(requireValue(arg), &config.capacityBytes)) {
        std::fprintf(stderr, "invalid --capacity value\n");
        return 1;
      }
    } else if (std::strcmp(arg, "--trace") == 0) {
      trace = true;
    } else if (std::strcmp(arg, "-h") == 0 || std::strcmp(arg, "--help") == 0) {
      printUsage(argv[0]);
      return 0;
    } else if (addrFile == nullptr) {
      addrFile = arg;
    } else {
      std::fprintf(stderr, "unexpected argument: %s\n", arg);
      printUsage(argv[0]);
      return 1;
    }
  }

  try {
    cachesim::Cache cache(config);

    std::ifstream file;
    std::istream *input = &std::cin;
    if (addrFile != nullptr) {
      file.open(addrFile);
      if (!file) {
        std::fprintf(stderr, "failed to open %s\n", addrFile);
        return 1;
      }
      input = &file;
    }

    std::string line;
    uint64_t lineNo = 0;
    while (std::getline(*input, line)) {
      lineNo++;
      if (line.empty() || line[0] == '#') continue;

      uint32_t addr = 0;
      if (!parseAddr(line, &addr)) {
        std::fprintf(stderr, "bad address at line %llu: %s\n",
                     static_cast<unsigned long long>(lineNo), line.c_str());
        return 1;
      }

      cachesim::AccessResult result = cache.access(addr);
      if (trace) {
        std::printf("addr=0x%08x tag=0x%x index=0x%x offset=0x%x way=%u %s",
                    addr, result.tag, result.index, result.offset, result.way, result.hit ? "hit" : "miss");
        if (result.evicted) {
          std::printf(" evictTag=0x%x", result.evictedTag);
        }
        std::printf("\n");
      }
    }

    const cachesim::CacheStats &stats = cache.stats();
    const cachesim::CacheConfig &actualConfig = cache.config();
    std::printf("Cache config: capacityBytes=%u ways=%u tagBits=%u indexBits=%u offsetBits=%u sets=%zu lines=%zu lineBytes=%u\n",
                actualConfig.capacityBytes,
                actualConfig.ways,
                actualConfig.tagBits,
                actualConfig.indexBits,
                actualConfig.offsetBits,
                cache.setCount(),
                cache.lineCount(),
                cache.lineBytes());
    std::printf("Cache stats: access=%llu hit=%llu miss=%llu hitRate=%.6f missRate=%.6f\n",
                static_cast<unsigned long long>(stats.accessCount),
                static_cast<unsigned long long>(stats.hitCount),
                static_cast<unsigned long long>(stats.missCount),
                stats.hitRate(),
                stats.missRate());
  } catch (const std::exception &e) {
    std::fprintf(stderr, "cachesim error: %s\n", e.what());
    return 1;
  }

  return 0;
}
