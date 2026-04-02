package mycpu.pipeline

final class ApiRef[T] {
  private var target: Option[T] = None

  def bind(x: T): Unit = {
    require(target.isEmpty, "ApiRef already bound")
    target = Some(x)
  }

  def get: T = target.getOrElse {
    throw new Exception("ApiRef not linked yet")
  }
}
