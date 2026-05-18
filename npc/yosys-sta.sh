make -C /home/nullstarfish/yosys-sta sta \
  DESIGN=myCore \
  CLK_FREQ_MHZ=5000 \
  CLK_PORT_NAME=clock \
  O=/home/nullstarfish/testSta \
  RTL_FILES="$(find /home/nullstarfish/personal-ysyx-workbench/npc/src/main/verilog/CoreSta \
    -maxdepth 1 \
    -name '*.sv' \
    -printf '%p ')"