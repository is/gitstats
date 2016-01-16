package us.yuxin.gitstats

object Scanner {
  @JvmStatic
  fun main(args:Array<String>) {
    run(args)
  }

  val cf = GSConfig.load()

  fun run(args:Array<String>) {
    println("hello world")
    println(cf)
  }
}