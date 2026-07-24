#ifndef NPC_RUNTIME_ILA_ENGINE_H
#define NPC_RUNTIME_ILA_ENGINE_H

#include <cstddef>
#include <cstdint>
#include <initializer_list>
#include <memory>
#include <string>

/**
 * @brief 单个 ILA 数据源的只读探针视图。
 *
 * 触发函数通过该类按探针名读取当前采样帧中的数据。视图本身不拥有
 * 采样数据，只保存访问上下文和回调函数，因此仅在对应触发函数执行期间有效。
 */
class IlaSourceView {
public:
  /** @brief 按 64 位字读取探针数据的回调函数类型。 */
  using ReadWord = uint64_t (*)(const void *context, const char *source,
                                const char *probe, size_t wordIndex);
  /** @brief 判断探针任意位是否非零的回调函数类型。 */
  using TestNonzero = bool (*)(const void *context, const char *source,
                               const char *probe);

  /**
   * @brief 构造指定数据源的只读视图。
   * @param context ILA 引擎提供的不透明帧上下文。
   * @param source 数据源名称。
   * @param readWord 按 64 位字读取探针的回调函数。
   * @param testNonzero 判断探针是否非零的回调函数。
   */
  IlaSourceView(const void *context, const char *source, ReadWord readWord,
                TestNonzero testNonzero);

  /**
   * @brief 读取探针最低 64 位。
   * @param probe 探针名称。
   * @return 探针的第 0 个 64 位字；不足 64 位时高位补零。
   */
  uint64_t operator[](const char *probe) const;

  /**
   * @brief 读取探针的指定 64 位字。
   * @param probe 探针名称。
   * @param wordIndex 从最低有效位开始计算的 64 位字索引。
   * @return 指定 64 位字；超出探针位宽时返回 0。
   */
  uint64_t word(const char *probe, size_t wordIndex) const;

  /**
   * @brief 判断探针是否包含任意非零位。
   * @param probe 探针名称。
   * @return 探针非零时返回 true，否则返回 false。
   */
  bool nonzero(const char *probe) const;

private:
  const void *context_;      ///< 不拥有所有权的帧上下文。
  const char *source_;       ///< 当前视图对应的数据源名称。
  ReadWord readWord_;        ///< 探针字读取回调。
  TestNonzero testNonzero_;  ///< 探针非零检测回调。
};

/**
 * @brief 一帧 ILA 原子采样数据的只读视图。
 *
 * 该视图是触发回调的入口，可按数据源名称继续取得 IlaSourceView。
 */
class IlaFrameView {
public:
  /** @brief 复用 IlaSourceView 的探针字读取回调类型。 */
  using ReadWord = IlaSourceView::ReadWord;
  /** @brief 复用 IlaSourceView 的探针非零检测回调类型。 */
  using TestNonzero = IlaSourceView::TestNonzero;

  /**
   * @brief 构造一帧采样数据的只读视图。
   * @param context ILA 引擎提供的不透明帧上下文。
   * @param readWord 按 64 位字读取探针的回调函数。
   * @param testNonzero 判断探针是否非零的回调函数。
   */
  IlaFrameView(const void *context, ReadWord readWord,
               TestNonzero testNonzero);

  /**
   * @brief 获取指定数据源的探针视图。
   * @param name 数据源名称。
   * @return 绑定到当前帧和指定数据源的只读视图。
   */
  IlaSourceView source(const char *name) const;

private:
  const void *context_;      ///< 不拥有所有权的帧上下文。
  ReadWord readWord_;        ///< 探针字读取回调。
  TestNonzero testNonzero_;  ///< 探针非零检测回调。
};

/**
 * @brief ILA 触发条件回调类型。
 * @param frame 当前原子采样帧的只读视图。
 * @return 当前帧满足触发条件时返回 true。
 */
using IlaTrigger = bool (*)(const IlaFrameView &frame);

/**
 * @brief NPC 仿真环境中的 DPI-ILA 采样、触发和 VCD 导出引擎。
 *
 * 引擎先接收 Chisel/DPI 注册的数据源和逐周期采样，再按照内置配置建立
 * capture。每个 capture 独立维护预触发环形历史、触发状态和后触发窗口。
 */
class IlaEngine {
public:
  /** @brief 创建一个尚未注册数据源和 capture 的 ILA 引擎。 */
  IlaEngine();

  /** @brief 销毁引擎及其持有的所有采样、capture 和配置数据。 */
  ~IlaEngine();

  /** @brief 禁止复制，避免采样缓冲和内部索引产生重复所有权。 */
  IlaEngine(const IlaEngine &) = delete;

  /** @brief 禁止复制赋值，避免采样缓冲和内部索引产生重复所有权。 */
  IlaEngine &operator=(const IlaEngine &) = delete;

  /**
   * @brief 注册或查询一个 DPI ILA 数据源。
   * @param name 数据源名称，必须是合法 C 标识符。
   * @param schema 逗号分隔的探针模式，单项格式为 "name:width"。
   * @param packedWidth packed vector 总位宽，至少 64 位且按 32 位对齐。
   * @return 注册成功时返回非负 source ID；参数或模式非法时返回 -1。
   *
   * 同名且 schema、位宽完全一致时返回已有 ID；同名但模式不一致时失败。
   */
  int allocateSource(const char *name, const char *schema, int packedWidth);

  /**
   * @brief 提交数据源在当前仿真时刻的 packed 采样值。
   * @param sourceId allocateSource() 返回的数据源 ID。
   * @param packed 按 32 位字排列的 packed vector，最低有效字在前。
   * @param timestamp Verilator 当前仿真时间，用作 VCD 时间戳。
   *
   * 未被任何 capture 观察的数据源会快速返回，不复制采样数据。
   */
  void sample(int sourceId, const uint32_t *packed, uint64_t timestamp);

  /**
   * @brief 创建一个由多个已注册数据源组成的 ILA capture。
   * @param name capture 名称，必须唯一且为合法 C 标识符。
   * @param sources 需要原子组合到同一帧的数据源名称列表。
   * @param depth 捕获窗口总帧数，最小为 2。
   * @param triggerPosition 期望的触发位置百分比，范围为 0..100。
   * @param triggerName 用于状态显示的触发条件文本。
   * @param trigger 已编译为 C++ 的触发判断回调。
   * @param output 捕获完成后写入的 VCD 路径。
   * @param enabled 初始是否启用触发判断。
   * @param error 配置失败时写入具体错误信息。
   * @return 配置成功返回 true，任一参数或引用的数据源非法时返回 false。
   */
  bool configureCapture(const char *name,
                        std::initializer_list<const char *> sources,
                        size_t depth, int triggerPosition,
                        const char *triggerName, IlaTrigger trigger,
                        const char *output, bool enabled,
                        std::string &error);

  /**
   * @brief 在 SDB 开始一次新的 c/si/fc/fsi 运行前准备 capture。
   *
   * 如果上一次运行已有 capture 完成，则统一清空所有 capture 的窗口并重新
   * 武装；尚未完成时保持现有预触发历史。
   */
  void prepareRun();

  /**
   * @brief 在一个 DUT 周期结束后组帧、判断触发并导出已完成的 VCD。
   * @return 若任一 capture 完成并要求仿真返回 SDB，则返回 true。
   */
  bool finishCycle();

  /**
   * @brief 启用或关闭指定 capture 的触发判断。
   * @param target capture 名称；nullptr、空字符串或 "all" 表示全部。
   * @param enabled true 为启用，false 为关闭。
   * @param error 未找到目标时写入错误信息。
   * @return 至少找到一个目标时返回 true，否则返回 false。
   *
   * 关闭处于后触发采样阶段的 capture 时，会取消本次捕获并重新初始化。
   */
  bool setTrigger(const char *target, bool enabled, std::string &error);

  /**
   * @brief 向标准输出打印 capture 的配置和运行状态。
   * @param target capture 名称；nullptr、空字符串或 "all" 表示全部。
   */
  void printStatus(const char *target = "all") const;

private:
  class Impl;                   ///< 隐藏具体数据结构和状态机的 PImpl 类型。
  std::unique_ptr<Impl> impl_;  ///< ILA 引擎内部实现的唯一所有权指针。
};

#endif