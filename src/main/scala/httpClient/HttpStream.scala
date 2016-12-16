package httpClient

/**
  * Created by iyanu on 12/9/16.
  */

import java.util.concurrent.{Executors, ThreadFactory, TimeUnit}

import json4sTest.JobState
import json4sTest.JobState._
import okhttp3.{OkHttpClient, Request, Response}
import okio.{Buffer, BufferedSource}
import org.json4s.{CustomSerializer, DefaultFormats, JString}
import org.json4s.JsonAST.JNull
import org.json4s.jackson.Serialization.read

import scala.concurrent.{Future, util => JUtil, _}
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import util.control.Breaks._

object SparkJobResource {
  case class Metadata(name: String,
                      uid: Option[String] = None,
                      labels: Option[Map[String, String]] = None,
                      annotations: Option[Map[String, String]] = None)

  case class SparkJobState(apiVersion: String,
                           kind: String,
                           metadata: Metadata,
                           spec: Map[String, Any])

  case class WatchObject(`type`: String, `object`: SparkJobState)

  case object JobStateSerDe extends CustomSerializer[JobState](_ =>
    ({
      case JString("SUBMITTED") => JobState.SUBMITTED
      case JString("QUEUED") => JobState.QUEUED
      case JString("RUNNING") => JobState.RUNNING
      case JString("FINISHED") => JobState.FINISHED
      case JString("KILLED") => JobState.KILLED
      case JString("FAILED") => JobState.FAILED
      case JNull =>
        throw new UnsupportedOperationException("No JobState Specified")
    }, {
      case JobState.FAILED => JString("FAILED")
      case JobState.SUBMITTED => JString("SUBMITTED")
      case JobState.KILLED => JString("KILLED")
      case JobState.FINISHED => JString("FINISHED")
      case JobState.QUEUED => JString("QUEUED")
      case JobState.RUNNING => JString("RUNNING")
    }))
}


object HttpStream extends App {

  import SparkJobResource._

  private lazy val blockingThreadPool = Executors.newCachedThreadPool(
    new ThreadFactory {
      def newThread(r: Runnable): Thread = {
        val thread = new Thread(r)
        thread.setDaemon(true)
        thread
      }
    }
  )

  private implicit val ec = ExecutionContext.fromExecutorService(blockingThreadPool)
  private implicit val formats = DefaultFormats + JobStateSerDe
  private val kind = "SparkJob"
  private val apiVersion = "apache.io/v1"
  private val protocol = "http://"
  private val apiEndpoint = s"${protocol}127.0.0.1:8001/apis/$apiVersion/namespaces/iobidele/sparkjobs"
  private val httpClient = new OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()

  val request = new Request.Builder().get().url(s"$apiEndpoint?watch=true").build()

  httpClient.newCall(request).execute() match {
    case r: Response if r.code() == 200 => doStuff(r)
    case _: Response => throw new IllegalStateException("There's fire on the mountain")
  }

  def doStuff(r: Response): Unit = {
    val temp = processResponse(r)
    println("I should see this before the object gets printed")
    temp onComplete {
      case Success(w: WatchObject) => println(w)
      case Failure(e: Throwable) => throw new IllegalStateException(e)
    }

    Await.result(temp, 1.day)
  }

  // Implementing the Server Sent Event Logic
  def processResponse(response: Response): Future[WatchObject] = {
    val p = Promise[WatchObject]()
    val buffer = new Buffer()
    val source: BufferedSource = response.body().source()

    // Returns true if there are no more bytes in this source. This will block until there are bytes
    // to read or the source is definitely exhausted.
    executeBlocking {
      breakable {
        while (!source.exhausted()) {
          source.read(buffer, 8192) match {
            case -1 =>
              p tryFailure new IllegalStateException("Source exhausted and object state is unknown")
              cleanUpListener(source, buffer, response)
            case _ => val wo = read[WatchObject](buffer.readUtf8())
              wo match {
                case WatchObject("DELETED", _) =>
                  println(wo)
                  //println("Finally i've been deleted. Cleaning up")
                  p trySuccess wo
                  cleanUpListener(source, buffer, response)
                case WatchObject(_, _) =>
              }
          }
        }
      }
    }
    println("This message should be printed first. Since the blocking watcher should go off on a new thread")
    p.future
  }


  def cleanUpListener(source: BufferedSource, buffer: Buffer, response: Response): Unit = {
    source.close()
    buffer.close()
    response.close()
    break()
  }

  private def executeBlocking[T](cb: => T): Future[T] = {
    val p = Promise[T]()

    blockingThreadPool.execute(
      new Runnable {
        override def run(): Unit = {
          try {
            p.trySuccess(blocking(cb))
          } catch {
            case e: Throwable => println(e.getMessage)
              p.tryFailure(e)
          }
        }
      })
    p.future
  }

}
