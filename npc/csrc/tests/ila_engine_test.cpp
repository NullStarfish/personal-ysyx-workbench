#include "runtime/services/ila_engine.h"

#include <cassert>
#include <filesystem>
#include <fstream>
#include <sstream>
#include <string>

namespace {

uint64_t triggerValue = 0;

bool valueTrigger(const IlaFrameView &frame) {
  return frame.source("source")["value"] == triggerValue;
}

bool ps2ReadTrigger(const IlaFrameView &frame) {
  const IlaSourceView ps2 = frame.source("ps2Chisel");
  return ps2["io_in_psel"] != 0 && ps2["io_in_penable"] != 0;
}

size_t countTimestamps(const std::string &path, size_t &triggerIndex) {
  std::ifstream input(path);
  std::string line;
  size_t timestamps = 0;
  triggerIndex = static_cast<size_t>(-1);

  while (std::getline(input, line)) {
    if (!line.empty() && line[0] == '#') {
      ++timestamps;
    }
    if (line == "1tr") {
      triggerIndex = timestamps - 1;
    }
  }
  return timestamps;
}

void checkTriggerPosition(int position, uint32_t triggerAt,
                          size_t expectedIndex) {
  const std::string path =
      "/tmp/npc-ila-position-" + std::to_string(position) + ".vcd";
  IlaEngine ila;
  const int source = ila.allocateSource("source", "value:32", 64);
  std::string error;

  triggerValue = triggerAt;
  assert(source >= 0);
  assert(ila.configureCapture("position", {"source"}, 8, position,
                              "valueTrigger", valueTrigger, path.c_str(),
                              true, error));

  for (uint32_t value = 0; value < 8; ++value) {
    uint32_t words[2] = {value, 0};
    ila.sample(source, words, value * 2);
    const bool stopped = ila.finishCycle();
    assert(stopped == (value == 7));
  }

  size_t actualIndex = 0;
  assert(countTimestamps(path, actualIndex) == 8);
  assert(actualIndex == expectedIndex);
}

void checkMultiSourceCapture() {
  const std::string path = "/tmp/npc-ila-ps2-read.vcd";
  IlaEngine ila;
  const int core = ila.allocateSource("Core", "io_master_araddr:32", 64);
  const int ps2 = ila.allocateSource(
      "ps2Chisel", "io_in_psel:1,io_in_penable:1,io_in_prdata:32", 64);
  std::string error;

  assert(core >= 0 && ps2 >= 0);
  assert(ila.configureCapture("ps2_read", {"Core", "ps2Chisel"}, 8, 50,
                              "ps2ReadTrigger", ps2ReadTrigger,
                              path.c_str(), true, error));

  for (uint32_t cycle = 0; cycle < 8; ++cycle) {
    uint32_t coreWords[2] = {0x10011000u, 0};
    uint32_t ps2Words[2] = {cycle == 4 ? 3u : 0u, 0x41u};
    ila.sample(core, coreWords, cycle * 2);
    ila.sample(ps2, ps2Words, cycle * 2);
    const bool stopped = ila.finishCycle();
    assert(stopped == (cycle == 7));
  }

  std::ifstream input(path);
  std::stringstream buffer;
  buffer << input.rdbuf();
  const std::string vcd = buffer.str();
  assert(vcd.find("$scope module ps2_read $end") != std::string::npos);
  assert(vcd.find("$scope module Core $end") != std::string::npos);
  assert(vcd.find("$scope module ps2Chisel $end") != std::string::npos);
  assert(vcd.find("io_master_araddr") != std::string::npos);
  assert(vcd.find("io_in_prdata") != std::string::npos);

  size_t actualIndex = 0;
  assert(countTimestamps(path, actualIndex) == 8);
  assert(actualIndex == 4);
}

void checkSourceAllocation() {
  IlaEngine ila;
  const int first = ila.allocateSource("source", "value:32", 64);
  const int duplicate = ila.allocateSource("source", "value:32", 64);
  const int conflict = ila.allocateSource("source", "other:32", 64);

  assert(first >= 0);
  assert(duplicate == first);
  assert(conflict < 0);
}

}  // namespace

int main() {
  checkSourceAllocation();
  checkTriggerPosition(0, 0, 0);
  checkTriggerPosition(50, 4, 4);
  checkTriggerPosition(75, 6, 6);
  checkTriggerPosition(100, 7, 7);
  checkTriggerPosition(50, 0, 0);
  checkMultiSourceCapture();
  return 0;
}