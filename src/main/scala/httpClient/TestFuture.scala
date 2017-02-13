package httpClient

import java.util.concurrent.{ExecutorService, Executors, TimeUnit}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration._
import scala.util.{Failure, Success}

/**
  * Created by iyanu on 12/26/16.
  */

object TestFuture extends App {


  implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))

  val c = new HttpStream

  val f = c.main()

  ec.execute(new Runnable {
    override def run(): Unit = {
      f onComplete {
        case Success(w) =>
          println(s"${Thread.currentThread().getName} I got here finally $w")
          ec.shutdown()
        case Failure(e: Throwable) =>
          println(s"${Thread.currentThread().getName} Future completed ${e.getMessage}")
          ec.shutdown()
      }
    }
  })

  println(s"${Thread.currentThread().getName} Going on to wait or do whatever")
  Thread.sleep(1000)
  c.closeSource()
  try {
    Await.result(f, Inf)
  } catch {
    case _: Throwable =>
  }

}
