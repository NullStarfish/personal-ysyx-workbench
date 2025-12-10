error id: file://<WORKSPACE>/rocket-chip/src/main/scala/prci/ClockDomain.scala:[1021..1025) in Input.VirtualFile("file://<WORKSPACE>/rocket-chip/src/main/scala/prci/ClockDomain.scala", "package freechips.rocketchip.prci

import chisel3._

import org.chipsalliance.cde.config._

import org.chipsalliance.diplomacy.lazymodule._

abstract class Domain(implicit p: Parameters) extends LazyModule with HasDomainCrossing
{
  def clockBundle: ClockBundle

  lazy val module = new Impl
  class Impl extends LazyRawModuleImp(this) {
    childClock := clockBundle.clock
    childReset := clockBundle.reset
    override def provideImplicitClockToLazyChildren = true

    // these are just for backwards compatibility with external devices
    // that were manually wiring themselves to the domain's clock/reset input:
    val clock = IO(Output(chiselTypeOf(clockBundle.clock)))
    val reset = IO(Output(chiselTypeOf(clockBundle.reset)))
    clock := clockBundle.clock
    reset := clockBundle.reset
  }
}

abstract class ClockDomain(implicit p: Parameters) extends Domain with HasClockDomainCrossing

class ClockSinkDomain(val clockSinkParams: ClockSinkParameters)(implicit p: Parameters) extends ClockDomain
{
  def this(take: Option[ClockParameters] = None, name: Option[String] = None)(implicit p: Parameters) = this(ClockSinkParameters(take = take, name = name))
  val clockNode = ClockSinkNode(Seq(clockSinkParams))
  def clockBundle = clockNode.in.head._1
  override lazy val desiredName = (clockSinkParams.name.toSeq :+ "ClockSinkDomain").mkString
}

class ClockSourceDomain(val clockSourceParams: ClockSourceParameters)(implicit p: Parameters) extends ClockDomain
{
  def this(give: Option[ClockParameters] = None, name: Option[String] = None)(implicit p: Parameters) = this(ClockSourceParameters(give = give, name = name))
  val clockNode = ClockSourceNode(Seq(clockSourceParams))
  def clockBundle = clockNode.out.head._1
  override lazy val desiredName = (clockSourceParams.name.toSeq :+ "ClockSourceDomain").mkString
}

abstract class ResetDomain(implicit p: Parameters) extends Domain with HasResetDomainCrossing
")
file://<WORKSPACE>/file:<WORKSPACE>/rocket-chip/src/main/scala/prci/ClockDomain.scala
file://<WORKSPACE>/rocket-chip/src/main/scala/prci/ClockDomain.scala:32: error: expected identifier; obtained this


Current stack trace:
java.base/java.lang.Thread.getStackTrace(Thread.java:2451)
scala.meta.internal.mtags.ScalaToplevelMtags.failMessage(ScalaToplevelMtags.scala:1206)
scala.meta.internal.mtags.ScalaToplevelMtags.$anonfun$reportError$1(ScalaToplevelMtags.scala:1192)
scala.meta.internal.metals.StdReporter.$anonfun$create$1(ReportContext.scala:148)
scala.util.Try$.apply(Try.scala:217)
scala.meta.internal.metals.StdReporter.create(ReportContext.scala:143)
scala.meta.pc.reports.Reporter.create(Reporter.java:10)
scala.meta.internal.mtags.ScalaToplevelMtags.reportError(ScalaToplevelMtags.scala:1189)
scala.meta.internal.mtags.ScalaToplevelMtags.newIdentifier(ScalaToplevelMtags.scala:1095)
scala.meta.internal.mtags.ScalaToplevelMtags.loop(ScalaToplevelMtags.scala:283)
scala.meta.internal.mtags.ScalaToplevelMtags.indexRoot(ScalaToplevelMtags.scala:96)
scala.meta.internal.metals.SemanticdbDefinition$.foreachWithReturnMtags(SemanticdbDefinition.scala:83)
scala.meta.internal.metals.Indexer.indexSourceFile(Indexer.scala:546)
scala.meta.internal.metals.Indexer.$anonfun$indexWorkspaceSources$7(Indexer.scala:366)
scala.meta.internal.metals.Indexer.$anonfun$indexWorkspaceSources$7$adapted(Indexer.scala:361)
scala.collection.IterableOnceOps.foreach(IterableOnce.scala:630)
scala.collection.IterableOnceOps.foreach$(IterableOnce.scala:628)
scala.collection.AbstractIterator.foreach(Iterator.scala:1313)
scala.collection.parallel.ParIterableLike$Foreach.leaf(ParIterableLike.scala:938)
scala.collection.parallel.Task.$anonfun$tryLeaf$1(Tasks.scala:52)
scala.runtime.java8.JFunction0$mcV$sp.apply(JFunction0$mcV$sp.scala:18)
scala.util.control.Breaks$$anon$1.catchBreak(Breaks.scala:97)
scala.collection.parallel.Task.tryLeaf(Tasks.scala:55)
scala.collection.parallel.Task.tryLeaf$(Tasks.scala:49)
scala.collection.parallel.ParIterableLike$Foreach.tryLeaf(ParIterableLike.scala:935)
scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.internal(Tasks.scala:169)
scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.internal$(Tasks.scala:156)
scala.collection.parallel.AdaptiveWorkStealingForkJoinTasks$AWSFJTWrappedTask.internal(Tasks.scala:304)
scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.compute(Tasks.scala:149)
scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.compute$(Tasks.scala:148)
scala.collection.parallel.AdaptiveWorkStealingForkJoinTasks$AWSFJTWrappedTask.compute(Tasks.scala:304)
java.base/java.util.concurrent.RecursiveAction.exec(RecursiveAction.java:194)
java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387)
java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312)
java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843)
java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808)
java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188)

  def this(take: Option[ClockParameters] = None, name: Option[String] = None)(implicit p: Parameters) = this(ClockSinkParameters(take = take, name = name))
      ^
#### Short summary: 

expected identifier; obtained this