#ifndef NPC_ILA_DEFAULT_CONFIG_H
#define NPC_ILA_DEFAULT_CONFIG_H

static inline bool ps2ReadTrigger(const IlaFrameView &frame) {
  const IlaSourceView ps2 = frame.source("ps2Chisel");

  return ps2["io_in_psel"] != 0 && ps2["io_in_penable"] != 0;
}

static inline bool configureDefaultIla(IlaEngine &ila, std::string &error) {
  return ila.configureCapture(
      "ps2_read",
      {"Core", "ps2Chisel"},
      4096,
      50,
      "ps2ReadTrigger",
      ps2ReadTrigger,
      "build/ila-ps2-read.vcd",
      true,
      error);
}

#define NPC_CONFIGURE_ILA(engine, error) configureDefaultIla(engine, error)

#endif