# Optional NVBoard archive.  Generated binding consumes the Verilated top type.

ifeq ($(BOARD),1)
ifeq ($(filter ysyxsoc ysyxsoc-bootloader,$(SIM_TARGET)),)
$(error BOARD=1 requires SIM_TARGET=ysyxsoc or ysyxsoc-bootloader)
endif

NVBOARD_SRC_DIR := $(NVBOARD_HOME)/src
NVBOARD_BUILD_DIR := $(SIM_BUILD_DIR)/nvboard
NVBOARD_ARCHIVE := $(NVBOARD_BUILD_DIR)/libnvboard.a
NVBOARD_SRCS := $(shell find $(NVBOARD_SRC_DIR) -name '*.cpp')
NVBOARD_OBJS := $(patsubst $(NVBOARD_SRC_DIR)/%.cpp,$(NVBOARD_BUILD_DIR)/%.o,$(NVBOARD_SRCS))
NXDC_FILES := $(CSRC_DIR)/constr/cons.nxdc
SRC_AUTO_BIND := $(SIM_BUILD_DIR)/auto_bind.cpp
AUTO_BIND_OBJ := $(NVBOARD_BUILD_DIR)/auto_bind.o

NVBOARD_INCLUDE_DIRS := $(NVBOARD_HOME)/usr/include
NVBOARD_CPPFLAGS := -DCONFIG_BOARD -DNVBOARD_RESOURCE_HOME=\"$(NVBOARD_HOME)\"
NVBOARD_LDLIBS := $(shell sdl2-config --libs) -lSDL2_image -lSDL2_ttf
NVBOARD_LIBS := $(NVBOARD_ARCHIVE)
CXXFLAGS += -O3 $(shell sdl2-config --cflags)

$(SRC_AUTO_BIND): $(NXDC_FILES) $(VERILATOR_MODEL_LIB) | $(SIM_BUILD_DIR)
	$(call build_banner,Generating NVBoard binding source...)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $(NXDC_FILES) $@

# NVBoard's implementation has a private nvboard.h; clients use usr/include.
$(NVBOARD_BUILD_DIR)/%.o: $(NVBOARD_SRC_DIR)/%.cpp $(BUILD_MODE_FILE) | $(SIM_BUILD_DIR)
	@mkdir -p $(@D)
	@echo "+ CXX -> NVBoard/$<"
	$(CXX) -I$(NVBOARD_HOME)/include $(CXXFLAGS) -MMD -MP -c -o $@ $<

$(AUTO_BIND_OBJ): $(SRC_AUTO_BIND) $(VERILATOR_MODEL_LIB) $(BUILD_MODE_FILE) | $(SIM_BUILD_DIR)
	@mkdir -p $(@D)
	@echo "+ CXX -> NVBoard/$<"
	$(CXX) $(CXXFLAGS) -MMD -MP -c -o $@ $<

$(NVBOARD_ARCHIVE): $(NVBOARD_OBJS) $(AUTO_BIND_OBJ)
	$(call build_banner,Archiving NVBoard runtime...)
	@mkdir -p $(@D)
	$(RM) $@
	$(AR) rcs $@ $^

-include $(NVBOARD_OBJS:.o=.d) $(AUTO_BIND_OBJ:.o=.d)

.PHONY: compdb-clean
compdb-clean::
	$(RM) $(NVBOARD_OBJS) $(AUTO_BIND_OBJ) $(NVBOARD_ARCHIVE)
endif
