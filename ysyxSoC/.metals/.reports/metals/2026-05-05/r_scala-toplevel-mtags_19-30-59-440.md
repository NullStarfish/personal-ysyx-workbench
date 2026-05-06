error id: file://<WORKSPACE>/rocket-chip/src/main/scala/prci/ClockDomain.scala:[1021,1025) in virtfile:file://<WORKSPACE>/rocket-chip/src/main/scala/prci/ClockDomain.scala(... n
{
  def this(take: Opt ...)
file://<WORKSPACE>/file:<WORKSPACE>/rocket-chip/src/main/scala/prci/ClockDomain.scala
file://<WORKSPACE>/rocket-chip/src/main/scala/prci/ClockDomain.scala:32: error: expected identifier; obtained this


Current stack trace:
java.base/java.lang.Thread.getStackTrace(Thread.java:2451)
scala.meta.internal.mtags.ScalaToplevelMtags.failMessage(ScalaToplevelMtags.scala:1250)
scala.meta.internal.mtags.ScalaToplevelMtags.$anonfun$reportError$1(ScalaToplevelMtags.scala:1236)
scala.meta.internal.metals.StdReporter.$anonfun$create$1(ReportContext.scala:148)
scala.util.Try$.apply(Try.scala:217)
scala.meta.internal.metals.StdReporter.create(ReportContext.scala:143)
scala.meta.pc.reports.Reporter.create(Reporter.java:10)
scala.meta.internal.mtags.ScalaToplevelMtags.reportError(ScalaToplevelMtags.scala:1233)
scala.meta.internal.mtags.ScalaToplevelMtags.newIdentifier(ScalaToplevelMtags.scala:1107)
scala.meta.internal.mtags.ScalaToplevelMtags.loop(ScalaToplevelMtags.scala:283)
scala.meta.internal.mtags.ScalaToplevelMtags.indexRoot(ScalaToplevelMtags.scala:96)
scala.meta.internal.metals.SemanticdbDefinition$.foreachWithReturnMtags(SemanticdbDefinition.scala:83)
scala.meta.internal.metals.Indexer.indexSourceFile(Indexer.scala:560)
scala.meta.internal.metals.Indexer.$anonfun$indexWorkspaceSources$7(Indexer.scala:380)
scala.meta.internal.metals.Indexer.$anonfun$indexWorkspaceSources$7$adapted(Indexer.scala:375)
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
scala.collection.parallel.AdaptiveWorkStealingTasks$AWSTWrappedTask.internal(Tasks.scala:159)
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