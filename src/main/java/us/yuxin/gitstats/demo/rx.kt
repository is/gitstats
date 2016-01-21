package us.yuxin.gitstats.demo

import rx.Observable
import rx.schedulers.Schedulers
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.concurrent.currentThread


object Rx0 {
  @JvmStatic
  fun main(args:Array<String>) {
    val s = listOf("abc", "def", "hijk")
    val generator = Observable.from(s)
    generator.subscribe {
      println(it)
    }
  }
}


object RxP0 {
  @JvmStatic
  fun main(args:Array<String>) {

    val ss = listOf("abc", "def", "hijk", "hloooo", "hasef")
    val executor = ThreadPoolExecutor(
      4, 4, 0, TimeUnit.SECONDS, LinkedBlockingQueue())

    val scheduler = Schedulers.from(executor)
    val ssg = Observable.from(ss).delay(0, TimeUnit.SECONDS, scheduler)

    ssg.subscribe {
      println(it + ":" + currentThread.name)
    }
  }
}