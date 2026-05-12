package mycpu.pipeline

object Rv32eEncoders {
  def encodeLui(rd: Int, imm20: Int): BigInt =
    (BigInt(imm20 & 0xfffff) << 12) | (BigInt(rd) << 7) | BigInt(0x37)

  def encodeAuipc(rd: Int, imm20: Int): BigInt =
    (BigInt(imm20 & 0xfffff) << 12) | (BigInt(rd) << 7) | BigInt(0x17)

  def encodeJal(rd: Int, imm: Int): BigInt = {
    val v = imm & 0x1fffff
    val bit20 = (v >> 20) & 1
    val bits10to1 = (v >> 1) & 0x3ff
    val bit11 = (v >> 11) & 1
    val bits19to12 = (v >> 12) & 0xff
    (BigInt(bit20) << 31) |
      (BigInt(bits19to12) << 12) |
      (BigInt(bit11) << 20) |
      (BigInt(bits10to1) << 21) |
      (BigInt(rd) << 7) |
      BigInt(0x6f)
  }

  def encodeJalr(rd: Int, rs1: Int, imm: Int): BigInt = {
    val imm12 = imm & 0xfff
    (BigInt(imm12) << 20) | (BigInt(rs1) << 15) | (BigInt(0) << 12) | (BigInt(rd) << 7) | BigInt(0x67)
  }

  def encodeBranch(funct3: Int, rs1: Int, rs2: Int, imm: Int): BigInt = {
    val v = imm & 0x1fff
    val bit12 = (v >> 12) & 1
    val bit11 = (v >> 11) & 1
    val bits10to5 = (v >> 5) & 0x3f
    val bits4to1 = (v >> 1) & 0xf
    (BigInt(bit12) << 31) |
      (BigInt(bits10to5) << 25) |
      (BigInt(rs2) << 20) |
      (BigInt(rs1) << 15) |
      (BigInt(funct3) << 12) |
      (BigInt(bits4to1) << 8) |
      (BigInt(bit11) << 7) |
      BigInt(0x63)
  }

  def encodeLoad(funct3: Int, rd: Int, rs1: Int, imm: Int): BigInt = {
    val imm12 = imm & 0xfff
    (BigInt(imm12) << 20) | (BigInt(rs1) << 15) | (BigInt(funct3) << 12) | (BigInt(rd) << 7) | BigInt(0x03)
  }

  def encodeStore(funct3: Int, rs2: Int, rs1: Int, imm: Int): BigInt = {
    val imm12 = imm & 0xfff
    val immHi = (imm12 >> 5) & 0x7f
    val immLo = imm12 & 0x1f
    (BigInt(immHi) << 25) |
      (BigInt(rs2) << 20) |
      (BigInt(rs1) << 15) |
      (BigInt(funct3) << 12) |
      (BigInt(immLo) << 7) |
      BigInt(0x23)
  }

  def encodeOpImm(funct3: Int, rd: Int, rs1: Int, imm: Int): BigInt = {
    val imm12 = imm & 0xfff
    (BigInt(imm12) << 20) | (BigInt(rs1) << 15) | (BigInt(funct3) << 12) | (BigInt(rd) << 7) | BigInt(0x13)
  }

  def encodeShiftImm(funct3: Int, funct7: Int, rd: Int, rs1: Int, shamt: Int): BigInt = {
    val imm12 = ((funct7 & 0x7f) << 5) | (shamt & 0x1f)
    encodeOpImm(funct3, rd, rs1, imm12)
  }

  def encodeOp(funct3: Int, funct7: Int, rd: Int, rs1: Int, rs2: Int): BigInt =
    (BigInt(funct7 & 0x7f) << 25) |
      (BigInt(rs2) << 20) |
      (BigInt(rs1) << 15) |
      (BigInt(funct3) << 12) |
      (BigInt(rd) << 7) |
      BigInt(0x33)

  def nop: BigInt = encodeOpImm(0, 0, 0, 0)
}
