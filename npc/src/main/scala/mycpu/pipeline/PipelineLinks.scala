package mycpu.pipeline

final class PipelineLinks {
  val fetch = new ApiRef[FetchApiDecl]
  val decode = new ApiRef[DecodeApiDecl]
  val execute = new ApiRef[ExecuteApiDecl]
  val regfile = new ApiRef[RegfileApiDecl]
  val memory = new ApiRef[MemoryApiDecl]
  val lsu = new ApiRef[LsuApiDecl]
  val writeback = new ApiRef[WritebackApiDecl]
}
