CXX ?= g++
CXXFLAGS ?= -std=c++17 -O2 -Wall -Wextra -Werror

TARGET := cachesim
SRCS := main.cpp cache_sim.cpp
OBJS := $(SRCS:.cpp=.o)

.PHONY: all clean test

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CXX) $(CXXFLAGS) -o $@ $^

%.o: %.cpp cache_sim.h
	$(CXX) $(CXXFLAGS) -c -o $@ $<

test: $(TARGET)
	printf "0x80000000\n0x80000004\n0x80000010\n0x80000000\n" | ./$(TARGET) --capacity 4096 --ways 1 --tag-bits 20 --index-bits 8 --offset-bits 4 --trace
	printf "0x80000000\n0x80001000\n0x80002000\n0x80000000\n" | ./$(TARGET) --capacity 64 --ways 2 --index-bits 1 --offset-bits 4 --trace

clean:
	rm -f $(TARGET) $(OBJS)
