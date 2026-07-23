#ifndef NPC_RUNTIME_ILA_ENGINE_H
#define NPC_RUNTIME_ILA_ENGINE_H

#include <cstddef>
#include <cstdint>
#include <initializer_list>
#include <memory>
#include <string>

class IlaSourceView {
public:
  using ReadWord = uint64_t (*)(const void *context, const char *source,
                                const char *probe, size_t wordIndex);
  using TestNonzero = bool (*)(const void *context, const char *source,
                               const char *probe);

  IlaSourceView(const void *context, const char *source, ReadWord readWord,
                TestNonzero testNonzero);

  uint64_t operator[](const char *probe) const;
  uint64_t word(const char *probe, size_t wordIndex) const;
  bool nonzero(const char *probe) const;

private:
  const void *context_;
  const char *source_;
  ReadWord readWord_;
  TestNonzero testNonzero_;
};

class IlaFrameView {
public:
  using ReadWord = IlaSourceView::ReadWord;
  using TestNonzero = IlaSourceView::TestNonzero;

  IlaFrameView(const void *context, ReadWord readWord,
               TestNonzero testNonzero);

  IlaSourceView source(const char *name) const;

private:
  const void *context_;
  ReadWord readWord_;
  TestNonzero testNonzero_;
};

using IlaTrigger = bool (*)(const IlaFrameView &frame);

class IlaEngine {
public:
  IlaEngine();
  ~IlaEngine();

  IlaEngine(const IlaEngine &) = delete;
  IlaEngine &operator=(const IlaEngine &) = delete;

  int allocateSource(const char *name, const char *schema, int packedWidth);
  void sample(int sourceId, const uint32_t *packed, uint64_t timestamp);

  bool configureCapture(const char *name,
                        std::initializer_list<const char *> sources,
                        size_t depth, int triggerPosition,
                        const char *triggerName, IlaTrigger trigger,
                        const char *output, bool enabled,
                        std::string &error);

  void prepareRun();
  bool finishCycle();
  bool setTrigger(const char *target, bool enabled, std::string &error);
  void printStatus(const char *target = "all") const;

private:
  class Impl;
  std::unique_ptr<Impl> impl_;
};

#endif