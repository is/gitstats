package us.yuxin.gitstats.demo

import rx.Observable


object Rx0 {
  @JvmStatic
  fun main(args:Array<String>) {
    val maxNumber = 5
    val s = listOf("abc", "def", "hijk")
    val generator = Observable.from(s)
    generator.subscribe {
      println(it)
    }
  }
}