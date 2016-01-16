package us.yuxin.gitstats

object Scanner {
  @JvmStatic
  fun main(args:Array<String>) {
    setupJschAgent()
    run(args)
  }

  final val C = GSConfig.load()
  fun run(@Suppress("UNUSED_PARAMETER") args:Array<String>) {
    for (repo in C.repositories) {
      updateRepo(C, repo)
    }
  }
}