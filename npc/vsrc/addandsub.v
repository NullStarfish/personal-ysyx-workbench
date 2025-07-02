module AddAndSub #(
    parameter WIDTH = 32
)   (
    input [WIDTH - 1 : 0] A, // A 和 B 是补码
    input [WIDTH - 1 : 0] B,
    input Cin, // 1 表示减法，0 表示加法
    output Carry,
    output zero,
    output Overflow,
    output [WIDTH - 1 : 0] Result
);
    wire [WIDTH - 1 : 0] t_Cin; 
    assign t_Cin = ({WIDTH{Cin}} ^ B) + {{(WIDTH -1){1'b0}}, Cin};  //应该先加上 Cin,不然在处理0的补码的时候会又carry
    assign {Carry, Result} = A + t_Cin;
    assign Overflow = (A[WIDTH - 1] == B[WIDTH - 1]) && (Result[WIDTH - 1] != A[WIDTH - 1]);
    assign zero = ~(|Result);
endmodule


//carry的定义：https://en.wikipedia.org/wiki/Carry_flag
//在减法时，我们发现a >= b时，carry = 0, a < b时，carry = 1
//在加法时，我们发现a + b >= 2^n时，carry = 1, a + b < 2^n时，carry = 0
//减法时，和借位相反，carry = 0时借位，carry = 1时不借位