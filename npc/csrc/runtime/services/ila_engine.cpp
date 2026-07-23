#include "runtime/services/ila_engine.h"

#include <algorithm>
#include <cctype>
#include <cstdio>
#include <deque>
#include <filesystem>
#include <fstream>
#include <limits>
#include <map>
#include <sstream>
#include <stdexcept>
#include <unordered_map>
#include <utility>
#include <vector>

namespace {

struct BigValue {
  std::vector<uint32_t> words;

  bool zero() const { return words.empty(); }

  bool bit(size_t index) const {
    const size_t wordIndex = index / 32;
    return wordIndex < words.size() &&
           ((words[wordIndex] >> (index % 32)) & 1u) != 0;
  }

  void normalize() {
    while (!words.empty() && words.back() == 0) {
      words.pop_back();
    }
  }
};

struct Probe {
  std::string name;
  size_t width = 0;
  size_t offset = 0;
};

struct Sample {
  uint64_t time = 0;
  std::vector<uint32_t> words;
};

struct Source {
  int id = -1;
  std::string name;
  std::string schema;
  int packedWidth = 0;
  std::vector<Probe> probes;
  bool watched = false;
  uint64_t sequence = 0;
  Sample latest;
};

struct Frame {
  uint64_t time = 0;
  std::vector<Sample> samples;
};

enum class CaptureState {
  Prefill,
  Armed,
  PostTrigger,
  Complete,
};

struct Capture {
  std::string name;
  std::vector<int> sourceIds;
  std::vector<uint64_t> lastSequences;

  size_t depth = 0;
  size_t triggerIndex = 0;
  size_t actualTriggerIndex = 0;
  size_t postRemaining = 0;

  std::string triggerText;
  std::string output;
  IlaTrigger trigger = nullptr;
  bool triggerEnabled = true;
  bool exported = false;
  CaptureState state = CaptureState::Prefill;

  std::deque<Frame> pre;
  std::vector<Frame> frames;
};

struct FrameContext {
  const std::vector<Source> *sources = nullptr;
  const Capture *capture = nullptr;
  const Frame *frame = nullptr;
};

bool isIdentifier(const std::string &name) {
  if (name.empty()) {
    return false;
  }

  const unsigned char first = static_cast<unsigned char>(name[0]);
  if (!(std::isalpha(first) || name[0] == '_')) {
    return false;
  }

  return std::all_of(name.begin() + 1, name.end(), [](unsigned char c) {
    return std::isalnum(c) || c == '_';
  });
}

BigValue extract(const Sample &sample, size_t offset, size_t width) {
  BigValue value;
  value.words.resize((width + 31) / 32);

  for (size_t bit = 0; bit < width; ++bit) {
    const size_t sourceBit = offset + bit;
    const bool set =
        ((sample.words[sourceBit / 32] >> (sourceBit % 32)) & 1u) != 0;
    if (set) {
      value.words[bit / 32] |= 1u << (bit % 32);
    }
  }

  value.normalize();
  return value;
}

size_t findSourceIndex(const FrameContext &context, const char *rawName) {
  const std::string name = rawName == nullptr ? "" : rawName;
  for (size_t index = 0; index < context.capture->sourceIds.size(); ++index) {
    const Source &source =
        (*context.sources)[context.capture->sourceIds[index]];
    if (source.name == name) {
      return index;
    }
  }
  throw std::runtime_error("capture does not watch source '" + name + "'");
}

const Probe &findProbe(const Source &source, const char *rawName) {
  const std::string name = rawName == nullptr ? "" : rawName;
  const auto found = std::find_if(
      source.probes.begin(), source.probes.end(),
      [&name](const Probe &probe) { return probe.name == name; });
  if (found == source.probes.end()) {
    throw std::runtime_error("unknown ILA probe '" + source.name + "." +
                             name + "'");
  }
  return *found;
}

uint64_t readProbeWord(const void *opaque, const char *sourceName,
                       const char *probeName, size_t wordIndex) {
  const auto &context = *static_cast<const FrameContext *>(opaque);
  const size_t sourceIndex = findSourceIndex(context, sourceName);
  const Source &source =
      (*context.sources)[context.capture->sourceIds[sourceIndex]];
  const Probe &probe = findProbe(source, probeName);
  const BigValue value =
      extract(context.frame->samples[sourceIndex], probe.offset, probe.width);

  const size_t lowWord = wordIndex * 2;
  if (lowWord >= value.words.size()) {
    return 0;
  }

  uint64_t result = value.words[lowWord];
  if (lowWord + 1 < value.words.size()) {
    result |= static_cast<uint64_t>(value.words[lowWord + 1]) << 32;
  }
  return result;
}

bool probeNonzero(const void *opaque, const char *sourceName,
                  const char *probeName) {
  const auto &context = *static_cast<const FrameContext *>(opaque);
  const size_t sourceIndex = findSourceIndex(context, sourceName);
  const Source &source =
      (*context.sources)[context.capture->sourceIds[sourceIndex]];
  const Probe &probe = findProbe(source, probeName);
  return !extract(context.frame->samples[sourceIndex], probe.offset,
                  probe.width)
              .zero();
}

const char *stateName(CaptureState state) {
  switch (state) {
    case CaptureState::Prefill:
      return "prefill";
    case CaptureState::Armed:
      return "armed";
    case CaptureState::PostTrigger:
      return "post-trigger";
    case CaptureState::Complete:
      return "complete";
  }
  return "unknown";
}

std::string signalId(size_t sourceIndex, size_t probeIndex) {
  return "v" + std::to_string(sourceIndex) + "_" +
         std::to_string(probeIndex);
}

}  // namespace

IlaSourceView::IlaSourceView(const void *context, const char *source,
                             ReadWord readWord, TestNonzero testNonzero)
    : context_(context),
      source_(source),
      readWord_(readWord),
      testNonzero_(testNonzero) {}

uint64_t IlaSourceView::operator[](const char *probe) const {
  return word(probe, 0);
}

uint64_t IlaSourceView::word(const char *probe, size_t wordIndex) const {
  return readWord_(context_, source_, probe, wordIndex);
}

bool IlaSourceView::nonzero(const char *probe) const {
  return testNonzero_(context_, source_, probe);
}

IlaFrameView::IlaFrameView(const void *context, ReadWord readWord,
                           TestNonzero testNonzero)
    : context_(context),
      readWord_(readWord),
      testNonzero_(testNonzero) {}

IlaSourceView IlaFrameView::source(const char *name) const {
  return IlaSourceView(context_, name, readWord_, testNonzero_);
}

class IlaEngine::Impl {
public:
  int allocateSource(const char *rawName, const char *rawSchema,
                     int packedWidth) {
    try {
      const std::string name = rawName == nullptr ? "" : rawName;
      const std::string schema = rawSchema == nullptr ? "" : rawSchema;
      if (!isIdentifier(name) || packedWidth < 64 || packedWidth % 32 != 0) {
        return -1;
      }

      const auto existing = sourceIds.find(name);
      if (existing != sourceIds.end()) {
        const Source &source = sources[existing->second];
        if (source.schema == schema && source.packedWidth == packedWidth) {
          return source.id;
        }
        fprintf(stderr, "ILA source '%s' was allocated with a new schema\n",
                name.c_str());
        return -1;
      }

      Source source;
      source.id = static_cast<int>(sources.size());
      source.name = name;
      source.schema = schema;
      source.packedWidth = packedWidth;

      size_t offset = 0;
      std::stringstream stream(schema);
      std::string item;
      std::map<std::string, bool> probeNames;
      while (std::getline(stream, item, ',')) {
        const size_t colon = item.find(':');
        if (colon == std::string::npos) {
          return -1;
        }

        Probe probe{
            item.substr(0, colon),
            std::stoull(item.substr(colon + 1)),
            offset,
        };
        if (!isIdentifier(probe.name) || probe.width == 0 ||
            probeNames[probe.name]) {
          return -1;
        }

        probeNames[probe.name] = true;
        offset += probe.width;
        source.probes.push_back(std::move(probe));
      }

      if (source.probes.empty() ||
          offset > static_cast<size_t>(packedWidth)) {
        return -1;
      }

      sourceIds.emplace(source.name, source.id);
      sources.push_back(std::move(source));
      return sources.back().id;
    } catch (const std::exception &error) {
      fprintf(stderr, "ILA source allocation failed: %s\n", error.what());
      return -1;
    }
  }

  void sample(int sourceId, const uint32_t *packed, uint64_t time) {
    if (sourceId < 0 || static_cast<size_t>(sourceId) >= sources.size() ||
        packed == nullptr) {
      return;
    }

    Source &source = sources[sourceId];
    if (!source.watched) {
      return;
    }

    source.latest.time = time;
    source.latest.words.assign(packed,
                               packed + source.packedWidth / 32);
    ++source.sequence;
  }

  void resetCapture(Capture &capture) {
    capture.pre.clear();
    capture.frames.clear();
    capture.actualTriggerIndex = 0;
    capture.postRemaining = 0;
    capture.exported = false;
    capture.state = capture.triggerIndex == 0 ? CaptureState::Armed
                                              : CaptureState::Prefill;
    capture.lastSequences.resize(capture.sourceIds.size());
    for (size_t index = 0; index < capture.sourceIds.size(); ++index) {
      capture.lastSequences[index] =
          sources[capture.sourceIds[index]].sequence;
    }
  }

  bool buildFrame(Capture &capture, Frame &frame) {
    uint64_t frameTime = 0;
    for (size_t index = 0; index < capture.sourceIds.size(); ++index) {
      const Source &source = sources[capture.sourceIds[index]];
      if (source.sequence <= capture.lastSequences[index]) {
        return false;
      }
      if (index == 0) {
        frameTime = source.latest.time;
      } else if (source.latest.time != frameTime) {
        return false;
      }
    }

    frame.time = frameTime;
    frame.samples.reserve(capture.sourceIds.size());
    for (size_t index = 0; index < capture.sourceIds.size(); ++index) {
      const Source &source = sources[capture.sourceIds[index]];
      frame.samples.push_back(source.latest);
      capture.lastSequences[index] = source.sequence;
    }
    return true;
  }

  void processFrame(Capture &capture, Frame frame) {
    if (capture.state == CaptureState::Complete) {
      return;
    }

    if (capture.state == CaptureState::PostTrigger) {
      capture.frames.push_back(std::move(frame));
      if (capture.postRemaining > 0) {
        --capture.postRemaining;
      }
      if (capture.postRemaining == 0) {
        capture.state = CaptureState::Complete;
      }
      return;
    }

    bool triggered = false;
    if (capture.triggerEnabled) {
      try {
        const FrameContext context{&sources, &capture, &frame};
        const IlaFrameView view(&context, readProbeWord, probeNonzero);
        triggered = capture.trigger(view);
      } catch (const std::exception &error) {
        fprintf(stderr, "ILA trigger error for %s: %s\n",
                capture.name.c_str(), error.what());
        capture.triggerEnabled = false;
      }
    }

    if (triggered) {
      capture.frames.assign(capture.pre.begin(), capture.pre.end());
      capture.actualTriggerIndex = capture.frames.size();
      capture.frames.push_back(std::move(frame));
      capture.postRemaining = capture.depth - capture.frames.size();
      capture.state = capture.postRemaining == 0
                          ? CaptureState::Complete
                          : CaptureState::PostTrigger;
      return;
    }

    if (capture.triggerIndex != 0) {
      capture.pre.push_back(std::move(frame));
      while (capture.pre.size() > capture.triggerIndex) {
        capture.pre.pop_front();
      }
    }
    capture.state = capture.pre.size() >= capture.triggerIndex
                        ? CaptureState::Armed
                        : CaptureState::Prefill;
  }

  bool exportVcd(Capture &capture, std::string &error) {
    try {
      const std::filesystem::path path(capture.output);
      if (path.has_parent_path()) {
        std::filesystem::create_directories(path.parent_path());
      }

      std::ofstream output(capture.output, std::ios::trunc);
      if (!output) {
        throw std::runtime_error("cannot open output");
      }

      output << "$date generated by NPC DPI-ILA $end\n"
             << "$version NPC DPI-ILA $end\n"
             << "$timescale 1ns $end\n"
             << "$scope module " << capture.name << " $end\n";

      for (size_t sourceIndex = 0;
           sourceIndex < capture.sourceIds.size(); ++sourceIndex) {
        const Source &source = sources[capture.sourceIds[sourceIndex]];
        output << "$scope module " << source.name << " $end\n";
        for (size_t probeIndex = 0; probeIndex < source.probes.size();
             ++probeIndex) {
          const Probe &probe = source.probes[probeIndex];
          output << "$var wire " << probe.width << " "
                 << signalId(sourceIndex, probeIndex) << " " << probe.name
                 << " $end\n";
        }
        output << "$upscope $end\n";
      }

      output << "$var wire 1 tr ila_trigger $end\n"
             << "$upscope $end\n"
             << "$enddefinitions $end\n";

      for (size_t frameIndex = 0; frameIndex < capture.frames.size();
           ++frameIndex) {
        const Frame &frame = capture.frames[frameIndex];
        output << "#" << frame.time << "\n";

        for (size_t sourceIndex = 0;
             sourceIndex < capture.sourceIds.size(); ++sourceIndex) {
          const Source &source = sources[capture.sourceIds[sourceIndex]];
          const Sample &sample = frame.samples[sourceIndex];
          for (size_t probeIndex = 0; probeIndex < source.probes.size();
               ++probeIndex) {
            const Probe &probe = source.probes[probeIndex];
            const BigValue value =
                extract(sample, probe.offset, probe.width);

            std::string bits;
            bits.reserve(probe.width);
            for (size_t bit = 0; bit < probe.width; ++bit) {
              bits.push_back(
                  value.bit(probe.width - 1 - bit) ? '1' : '0');
            }
            output << "b" << bits << " "
                   << signalId(sourceIndex, probeIndex) << "\n";
          }
        }

        output << (frameIndex == capture.actualTriggerIndex ? '1' : '0')
               << "tr\n";
      }

      if (!output) {
        throw std::runtime_error("write failed");
      }
      return true;
    } catch (const std::exception &exception) {
      error = exception.what();
      return false;
    }
  }

  std::vector<Source> sources;
  std::unordered_map<std::string, int> sourceIds;
  std::vector<Capture> captures;
  std::unordered_map<std::string, size_t> captureIds;
};

IlaEngine::IlaEngine() : impl_(std::make_unique<Impl>()) {}

IlaEngine::~IlaEngine() = default;

int IlaEngine::allocateSource(const char *name, const char *schema,
                              int packedWidth) {
  return impl_->allocateSource(name, schema, packedWidth);
}

void IlaEngine::sample(int sourceId, const uint32_t *packed,
                       uint64_t timestamp) {
  impl_->sample(sourceId, packed, timestamp);
}

bool IlaEngine::configureCapture(
    const char *rawName, std::initializer_list<const char *> sourceNames,
    size_t depth, int triggerPosition, const char *triggerName,
    IlaTrigger trigger, const char *output, bool enabled,
    std::string &error) {
  try {
    const std::string name = rawName == nullptr ? "" : rawName;
    if (!isIdentifier(name)) {
      throw std::runtime_error("invalid ILA capture name '" + name + "'");
    }
    if (impl_->captureIds.find(name) != impl_->captureIds.end()) {
      throw std::runtime_error("ILA capture '" + name +
                               "' is already configured");
    }
    if (sourceNames.size() == 0) {
      throw std::runtime_error("ILA capture must watch at least one source");
    }
    if (depth < 2) {
      throw std::runtime_error("depth must be at least 2");
    }
    if (triggerPosition < 0 || triggerPosition > 100) {
      throw std::runtime_error("trigger_position must be 0..100");
    }
    if (trigger == nullptr) {
      throw std::runtime_error("trigger callback must not be null");
    }

    const std::string outputPath = output == nullptr ? "" : output;
    if (outputPath.empty()) {
      throw std::runtime_error("output must not be empty");
    }

    Capture capture;
    capture.name = name;
    capture.depth = depth;
    capture.triggerText =
        triggerName == nullptr ? "<compiled>" : triggerName;
    capture.trigger = trigger;
    capture.output = outputPath;
    capture.triggerEnabled = enabled;

    size_t wordsPerFrame = 0;
    std::map<int, bool> selectedSources;
    for (const char *rawSource : sourceNames) {
      const std::string sourceName = rawSource == nullptr ? "" : rawSource;
      const auto found = impl_->sourceIds.find(sourceName);
      if (found == impl_->sourceIds.end()) {
        throw std::runtime_error("unknown ILA source '" + sourceName + "'");
      }
      if (selectedSources[found->second]) {
        throw std::runtime_error("duplicate ILA source '" + sourceName + "'");
      }

      selectedSources[found->second] = true;
      capture.sourceIds.push_back(found->second);
      wordsPerFrame +=
          static_cast<size_t>(impl_->sources[found->second].packedWidth) / 32;
    }

    if (wordsPerFrame != 0 &&
        depth > std::numeric_limits<size_t>::max() / wordsPerFrame) {
      throw std::runtime_error("capture depth overflows host memory size");
    }

    const size_t percentage = static_cast<size_t>(triggerPosition);
    const size_t desiredIndex =
        (depth / 100) * percentage + ((depth % 100) * percentage) / 100;
    capture.triggerIndex = std::min(depth - 1, desiredIndex);

    for (int sourceId : capture.sourceIds) {
      impl_->sources[sourceId].watched = true;
    }
    impl_->resetCapture(capture);

    const size_t captureId = impl_->captures.size();
    impl_->captureIds.emplace(capture.name, captureId);
    impl_->captures.push_back(std::move(capture));
    return true;
  } catch (const std::exception &exception) {
    error = exception.what();
    return false;
  }
}

void IlaEngine::prepareRun() {
  const bool completed = std::any_of(
      impl_->captures.begin(), impl_->captures.end(),
      [](const Capture &capture) {
        return capture.state == CaptureState::Complete;
      });
  if (!completed) {
    return;
  }

  for (Capture &capture : impl_->captures) {
    impl_->resetCapture(capture);
  }
}

bool IlaEngine::finishCycle() {
  for (Capture &capture : impl_->captures) {
    if (capture.state == CaptureState::Complete) {
      continue;
    }

    Frame frame;
    if (impl_->buildFrame(capture, frame)) {
      impl_->processFrame(capture, std::move(frame));
    }
  }

  bool stop = false;
  for (Capture &capture : impl_->captures) {
    if (capture.state != CaptureState::Complete || capture.exported) {
      continue;
    }

    std::string error;
    if (impl_->exportVcd(capture, error)) {
      capture.exported = true;
      stop = true;
      printf("ILA triggered: capture=%s sample=%zu/%zu time=%llu\n",
             capture.name.c_str(), capture.actualTriggerIndex, capture.depth,
             static_cast<unsigned long long>(
                 capture.frames[capture.actualTriggerIndex].time));
      printf("ILA capture written: %s\n", capture.output.c_str());
    } else {
      fprintf(stderr, "ILA export failed for %s: %s\n",
              capture.name.c_str(), error.c_str());
      capture.exported = true;
      stop = true;
    }
  }
  return stop;
}

bool IlaEngine::setTrigger(const char *target, bool enabled,
                           std::string &error) {
  const std::string targetName =
      target == nullptr || *target == '\0' ? "all" : target;
  bool found = false;

  for (Capture &capture : impl_->captures) {
    if (targetName != "all" && capture.name != targetName) {
      continue;
    }

    found = true;
    capture.triggerEnabled = enabled;
    if (!enabled && capture.state == CaptureState::PostTrigger) {
      impl_->resetCapture(capture);
    }
  }

  if (!found) {
    error = "unknown ILA capture '" + targetName + "'";
    return false;
  }
  return true;
}

void IlaEngine::printStatus(const char *target) const {
  const std::string targetName =
      target == nullptr || *target == '\0' ? "all" : target;
  bool found = false;

  for (const Capture &capture : impl_->captures) {
    if (targetName != "all" && capture.name != targetName) {
      continue;
    }

    found = true;
    const bool capturing = capture.state == CaptureState::PostTrigger ||
                           capture.state == CaptureState::Complete;
    const size_t samples =
        capturing ? capture.frames.size() : capture.pre.size();

    printf("ILA %-16s state=%-12s samples=%zu/%zu trigger_target=%zu",
           capture.name.c_str(), stateName(capture.state), samples,
           capture.depth, capture.triggerIndex);
    if (capturing) {
      printf(" trigger_actual=%zu", capture.actualTriggerIndex);
    }
    printf(" enabled=%s\n  sources:",
           capture.triggerEnabled ? "on" : "off");
    for (int sourceId : capture.sourceIds) {
      printf(" %s", impl_->sources[sourceId].name.c_str());
    }
    printf("\n  trigger: %s\n  output: %s\n",
           capture.triggerText.c_str(), capture.output.c_str());
  }

  if (!found) {
    printf("No configured ILA capture matches '%s'\n", targetName.c_str());
  }
}