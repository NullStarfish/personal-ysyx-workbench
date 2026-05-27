# Cache reference model linked into the simulation executable.

CACHESIM_INCLUDE_DIRS := $(CACHESIM_DIR)
CACHESIM_BUILD_DIR := $(SIM_BUILD_DIR)/cachesim
CACHESIM_CPP_SRCS := $(CACHESIM_DIR)/cache_sim.cpp
CACHESIM_OBJS := $(patsubst $(CACHESIM_DIR)/%.cpp,$(CACHESIM_BUILD_DIR)/%.o,$(CACHESIM_CPP_SRCS))
CACHESIM_ARCHIVE := $(CACHESIM_BUILD_DIR)/libcachesim.a
CACHESIM_LIBS := $(CACHESIM_ARCHIVE)

$(CACHESIM_BUILD_DIR)/%.o: $(CACHESIM_DIR)/%.cpp | $(SIM_BUILD_DIR)
	@mkdir -p $(@D)
	@echo "+ CXX -> cachesim/$<"
	$(CXX) $(CXXFLAGS) -MMD -MP -c -o $@ $<

$(CACHESIM_ARCHIVE): $(CACHESIM_OBJS)
	$(call build_banner,Archiving cache reference model...)
	@mkdir -p $(@D)
	$(RM) $@
	$(AR) rcs $@ $^

-include $(CACHESIM_OBJS:.o=.d)

.PHONY: compdb-clean
compdb-clean::
	$(RM) $(CACHESIM_OBJS) $(CACHESIM_ARCHIVE)
