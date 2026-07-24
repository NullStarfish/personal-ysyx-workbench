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

/**
 * @brief 保存任意位宽无符号值的低字在前 32 位字数组。
 *
 * 该类型用于读取和导出位宽可能超过 64 位的 ILA 探针，尾部的零字会被
 * 规范化移除，以便高效判断零值和访问有效位。
 */
struct BigValue {
  std::vector<uint32_t> words;  ///< 从最低有效位开始排列的 32 位字。

  /**
   * @brief 判断当前多字整数是否为零。
   * @return 规范化后的字数组为空时返回 true。
   */
  bool zero() const { return words.empty(); }

  /**
   * @brief 读取指定二进制位。
   * @param index 从最低有效位开始计算的位索引。
   * @return 该位为 1 时返回 true；越界位按 0 处理。
   */
  bool bit(size_t index) const {
    const size_t wordIndex = index / 32;
    return wordIndex < words.size() &&
           ((words[wordIndex] >> (index % 32)) & 1u) != 0;
  }

  /** @brief 移除最高端连续的零字，维持 BigValue 的规范形式。 */
  void normalize() {
    while (!words.empty() && words.back() == 0) {
      words.pop_back();
    }
  }
};

/** @brief 描述一个探针在数据源 packed vector 中的名称和位段位置。 */
struct Probe {
  std::string name;    ///< 数据源内部唯一的探针名称。
  size_t width = 0;    ///< 探针位宽。
  size_t offset = 0;   ///< 探针最低位在 packed vector 中的位偏移。
};

/** @brief 保存某个数据源在一个仿真时刻提交的原始采样。 */
struct Sample {
  uint64_t time = 0;            ///< Verilator 仿真时间戳。
  std::vector<uint32_t> words;  ///< packed vector 的低字在前 32 位字数组。
};

/** @brief 一个由 Chisel/DPI 注册的 ILA 原子采样数据源。 */
struct Source {
  int id = -1;                  ///< 在 IlaEngine 内部稳定的数据源编号。
  std::string name;             ///< 数据源名称。
  std::string schema;           ///< 原始 "probe:width,..." 模式字符串。
  int packedWidth = 0;          ///< DPI packed vector 的对齐后总位宽。
  std::vector<Probe> probes;    ///< 按 packed 顺序解析出的探针描述。
  bool watched = false;         ///< 是否至少被一个 capture 观察。
  uint64_t sequence = 0;        ///< 每次接受有效采样后递增的序列号。
  Sample latest;                ///< 最近一次有效采样。
};

/** @brief 同一时间戳下多个数据源组成的一帧原子采样。 */
struct Frame {
  uint64_t time = 0;             ///< 本帧所有数据源共同的仿真时间戳。
  std::vector<Sample> samples;   ///< 顺序与 Capture::sourceIds 一致的采样。
};

/** @brief ILA capture 的采样状态机。 */
enum class CaptureState {
  Prefill,      ///< 预触发历史尚未达到目标长度。
  Armed,        ///< 预触发历史已就绪，等待触发条件命中。
  PostTrigger,  ///< 已命中触发条件，继续收集后触发样本。
  Complete,     ///< 捕获窗口完整，可以导出并请求仿真暂停。
};

/**
 * @brief 一项独立 ILA 捕获任务的配置、状态机和采样窗口。
 *
 * capture 可组合多个 Source，但仅在所有数据源都提交了相同时间戳的新采样时
 * 才生成 Frame。触发前使用 pre 保存环形历史，触发后使用 frames 保存最终窗口。
 */
struct Capture {
  std::string name;                       ///< capture 的唯一名称。
  std::vector<int> sourceIds;             ///< 参与组帧的数据源 ID。
  std::vector<uint64_t> lastSequences;    ///< 各数据源上次消费的序列号。

  size_t depth = 0;               ///< 捕获窗口的目标总帧数。
  size_t triggerIndex = 0;        ///< 配置期望的触发样本索引。
  size_t actualTriggerIndex = 0;  ///< 实际窗口中的触发样本索引。
  size_t postRemaining = 0;       ///< 触发后仍需采集的帧数。

  std::string triggerText;                  ///< 用于状态显示的触发条件文本。
  std::string output;                       ///< VCD 输出路径。
  IlaTrigger trigger = nullptr;             ///< 已编译的触发判断函数。
  bool triggerEnabled = true;               ///< 是否执行触发判断。
  bool exported = false;                    ///< 完成窗口是否已尝试导出。
  CaptureState state = CaptureState::Prefill;  ///< 当前采样状态。

  std::deque<Frame> pre;          ///< 触发前的定长环形历史。
  std::vector<Frame> frames;      ///< 触发后形成的最终捕获窗口。
};

/** @brief 触发视图回调访问当前引擎数据所需的不透明上下文。 */
struct FrameContext {
  const std::vector<Source> *sources = nullptr;  ///< 全部已注册数据源。
  const Capture *capture = nullptr;              ///< 正在判断触发的 capture。
  const Frame *frame = nullptr;                  ///< 正在判断触发的当前帧。
};

/**
 * @brief 检查名称是否满足 ILA 配置允许的 C 标识符格式。
 * @param name 待检查的名称。
 * @return 首字符为字母或下划线，且其余字符均为字母、数字或下划线时返回 true。
 */
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

/**
 * @brief 从 packed 采样中提取任意位宽的连续位段。
 * @param sample 原始数据源采样。
 * @param offset 目标位段最低位在 packed vector 中的偏移。
 * @param width 目标位段宽度。
 * @return 低字在前并已规范化的任意位宽无符号值。
 */
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

/**
 * @brief 查找数据源在当前 capture 组帧顺序中的索引。
 * @param context 当前触发判断上下文。
 * @param rawName 数据源名称；nullptr 按空名称处理。
 * @return 数据源在 Capture::sourceIds 和 Frame::samples 中的索引。
 * @throws std::runtime_error 当前 capture 未观察指定数据源时抛出。
 */
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

/**
 * @brief 在指定数据源内按名称查找探针描述。
 * @param source 待查询的数据源。
 * @param rawName 探针名称；nullptr 按空名称处理。
 * @return 匹配的探针描述引用。
 * @throws std::runtime_error 数据源中不存在该探针时抛出。
 */
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

/**
 * @brief 为 IlaSourceView 读取探针的指定 64 位字。
 * @param opaque 指向 FrameContext 的不透明指针。
 * @param sourceName 数据源名称。
 * @param probeName 探针名称。
 * @param wordIndex 从最低有效位开始计算的 64 位字索引。
 * @return 指定 64 位字；超出探针位宽时返回 0。
 * @throws std::runtime_error 数据源或探针不存在时抛出。
 */
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

/**
 * @brief 为 IlaSourceView 判断任意位宽探针是否非零。
 * @param opaque 指向 FrameContext 的不透明指针。
 * @param sourceName 数据源名称。
 * @param probeName 探针名称。
 * @return 探针包含任意非零位时返回 true。
 * @throws std::runtime_error 数据源或探针不存在时抛出。
 */
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

/**
 * @brief 将 capture 状态转换为 SDB 状态输出使用的固定字符串。
 * @param state capture 状态枚举值。
 * @return 静态生命周期的状态名称字符串。
 */
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

/**
 * @brief 为 VCD 中的探针生成 capture 内唯一的信号标识符。
 * @param sourceIndex 数据源在 capture 中的索引。
 * @param probeIndex 探针在数据源中的索引。
 * @return 形如 "v<source>_<probe>" 的 VCD 标识符。
 */
std::string signalId(size_t sourceIndex, size_t probeIndex) {
  return "v" + std::to_string(sourceIndex) + "_" +
         std::to_string(probeIndex);
}

}  // namespace

/**
 * @brief 保存数据源视图所需的上下文和访问回调。
 * @param context ILA 引擎提供的不透明帧上下文。
 * @param source 当前视图绑定的数据源名称。
 * @param readWord 探针字读取回调。
 * @param testNonzero 探针非零检测回调。
 */
IlaSourceView::IlaSourceView(const void *context, const char *source,
                             ReadWord readWord, TestNonzero testNonzero)
    : context_(context),
      source_(source),
      readWord_(readWord),
      testNonzero_(testNonzero) {}

/**
 * @brief 通过下标语法读取探针最低 64 位。
 * @param probe 探针名称。
 * @return 探针的第 0 个 64 位字。
 */
uint64_t IlaSourceView::operator[](const char *probe) const {
  return word(probe, 0);
}

/**
 * @brief 通过引擎回调读取探针的指定 64 位字。
 * @param probe 探针名称。
 * @param wordIndex 64 位字索引。
 * @return 回调返回的探针字。
 */
uint64_t IlaSourceView::word(const char *probe, size_t wordIndex) const {
  return readWord_(context_, source_, probe, wordIndex);
}

/**
 * @brief 通过引擎回调判断探针是否非零。
 * @param probe 探针名称。
 * @return 回调返回的非零判断结果。
 */
bool IlaSourceView::nonzero(const char *probe) const {
  return testNonzero_(context_, source_, probe);
}

/**
 * @brief 保存帧视图所需的上下文和探针访问回调。
 * @param context ILA 引擎提供的不透明帧上下文。
 * @param readWord 探针字读取回调。
 * @param testNonzero 探针非零检测回调。
 */
IlaFrameView::IlaFrameView(const void *context, ReadWord readWord,
                           TestNonzero testNonzero)
    : context_(context),
      readWord_(readWord),
      testNonzero_(testNonzero) {}

/**
 * @brief 构造绑定到当前帧和指定数据源的只读视图。
 * @param name 数据源名称。
 * @return 可继续按探针名读取数据的 IlaSourceView。
 */
IlaSourceView IlaFrameView::source(const char *name) const {
  return IlaSourceView(context_, name, readWord_, testNonzero_);
}

/**
 * @brief IlaEngine 的内部实现，持有数据源、capture 和采样状态机。
 *
 * PImpl 将 STL 容器和 VCD 导出细节隔离在实现文件中，保持公开头文件稳定。
 */
class IlaEngine::Impl {
public:
  /**
   * @brief 校验 schema 并注册一个 ILA 数据源。
   * @param rawName 数据源名称；nullptr 按空字符串处理。
   * @param rawSchema 逗号分隔的 "probe:width" 模式字符串。
   * @param packedWidth packed vector 的对齐后总位宽。
   * @return 成功时返回数据源 ID，校验或解析失败时返回 -1。
   */
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

  /**
   * @brief 保存被观察数据源的最新 packed 采样并推进序列号。
   * @param sourceId 数据源 ID。
   * @param packed 低字在前的 32 位 packed 数据指针。
   * @param time 当前 Verilator 仿真时间。
   */
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

  /**
   * @brief 清空捕获窗口并按配置的触发位置重新初始化状态机。
   * @param capture 需要重新武装的 capture。
   *
   * 同时记录各数据源当前序列号，防止重新运行后重复消费旧采样。
   */
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

  /**
   * @brief 将多个数据源相同时间戳的新采样组合为一帧。
   * @param capture 指定数据源集合和上次消费序列号的 capture。
   * @param frame 成功时写入构造完成的帧。
   * @return 所有数据源都有未消费且时间戳一致的采样时返回 true。
   */
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

  /**
   * @brief 将一帧新采样送入 capture 状态机并判断触发条件。
   * @param capture 接收该帧的 capture。
   * @param frame 当前原子采样帧，函数可将其移动到历史或最终窗口。
   *
   * 触发前维护定长 pre 队列；触发命中后固定实际触发索引，并继续采集
   * postRemaining 帧。触发回调抛出异常时会禁用该 capture 的触发判断。
   */
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

  /**
   * @brief 将完整 capture 窗口覆盖写入只包含 ILA 探针的 VCD 文件。
   * @param capture 已进入 Complete 状态且包含最终帧窗口的 capture。
   * @param error 导出失败时写入文件系统或输出流错误信息。
   * @return VCD 完整写入成功时返回 true，否则返回 false。
   */
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

  std::vector<Source> sources;  ///< 按 source ID 索引的全部数据源。
  std::unordered_map<std::string, int> sourceIds;  ///< 名称到 source ID 的索引。
  std::vector<Capture> captures;  ///< 按 capture ID 索引的全部捕获任务。
  std::unordered_map<std::string, size_t> captureIds;  ///< 名称到 capture ID 的索引。
};

/** @brief 创建内部实现对象并初始化空的 ILA 引擎。 */
IlaEngine::IlaEngine() : impl_(std::make_unique<Impl>()) {}

/** @brief 通过 unique_ptr 自动释放内部数据源、capture 和采样缓冲。 */
IlaEngine::~IlaEngine() = default;

/**
 * @brief 将数据源注册请求转发给内部实现。
 * @param name 数据源名称。
 * @param schema 探针模式字符串。
 * @param packedWidth packed vector 总位宽。
 * @return 内部实现返回的数据源 ID，失败时为 -1。
 */
int IlaEngine::allocateSource(const char *name, const char *schema,
                              int packedWidth) {
  return impl_->allocateSource(name, schema, packedWidth);
}

/**
 * @brief 将一次 DPI 采样提交给内部数据源存储。
 * @param sourceId 数据源 ID。
 * @param packed 低字在前的 packed 数据指针。
 * @param timestamp Verilator 仿真时间戳。
 */
void IlaEngine::sample(int sourceId, const uint32_t *packed,
                       uint64_t timestamp) {
  impl_->sample(sourceId, packed, timestamp);
}

/**
 * @brief 校验配置并创建一个新的 ILA capture。
 * @param rawName capture 名称；nullptr 按空字符串处理。
 * @param sourceNames 参与原子组帧的数据源名称列表。
 * @param depth 捕获窗口总帧数。
 * @param triggerPosition 期望触发位置百分比，范围为 0..100。
 * @param triggerName 用于状态输出的触发条件文本。
 * @param trigger 已编译的触发回调。
 * @param output VCD 输出路径。
 * @param enabled 初始是否启用触发判断。
 * @param error 配置失败时写入具体错误原因。
 * @return 配置成功返回 true，否则返回 false。
 *
 * 函数还会检查重复数据源和主机 size_t 乘法溢出，计算目标触发索引，
 * 标记被观察的数据源，并将新 capture 初始化为 Prefill 或 Armed 状态。
 */
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

/**
 * @brief 在新一轮 SDB 运行前重新武装已经完成的捕获任务。
 *
 * 只有至少一个 capture 已完成时才统一复位全部 capture，以保证多个 capture
 * 在下一轮运行中从同一起点重新采集；否则保留当前预触发历史。
 */
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

/**
 * @brief 在当前 DUT 周期结束后推进所有 capture 并处理完成事件。
 * @return 任一 capture 完成导出或导出失败、需要停止仿真时返回 true。
 *
 * 函数先尝试为每个 capture 构造新帧并推进状态机，再统一导出本周期完成的
 * capture，确保同一周期完成的多个任务都能写出结果。
 */
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

/**
 * @brief 修改一个或全部 capture 的触发使能状态。
 * @param target capture 名称；nullptr、空字符串或 "all" 表示全部。
 * @param enabled 新的触发使能状态。
 * @param error 未找到目标时写入错误信息。
 * @return 找到并更新至少一个 capture 时返回 true，否则返回 false。
 */
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

/**
 * @brief 打印一个或全部 capture 的配置、状态和当前采样进度。
 * @param target capture 名称；nullptr、空字符串或 "all" 表示全部。
 *
 * 输出包括状态机状态、窗口进度、目标及实际触发索引、触发开关、数据源、
 * 触发条件文本和 VCD 输出路径。
 */
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