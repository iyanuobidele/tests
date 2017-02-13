package httpClient

/**
  * Created by iyanu on 12/9/16.
  */

import java.util.concurrent.TimeUnit

import json4sTest.JobState
import json4sTest.JobState._
import okhttp3.{OkHttpClient, Request, Response, ResponseBody}
import okio.{Buffer, BufferedSource}
import org.json4s.{CustomSerializer, DefaultFormats, JString}
import org.json4s.JsonAST.JNull
import org.json4s.jackson.Serialization.read

import scala.concurrent.{Future, _}
import scala.util.control.Breaks._

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


class HttpStream()(implicit ec: ExecutionContext) {

  import SparkJobResource._

  // private implicit val ec = ExecutionContext.fromExecutorService(blockingThreadPool)
  private implicit val formats = DefaultFormats + JobStateSerDe
  private val apiVersion = "apache.io/v1"
  private val protocol = "http://"
  private val apiEndpoint = s"${protocol}127.0.0.1:8001/apis/$apiVersion/namespaces/iobidele/sparkjobs"
  private val httpClient = new OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()

  val request: Request = new Request.Builder().get().url(s"$apiEndpoint?watch=true").build()

  var resp: Response = _

  def main(): Future[WatchObject] = {
    httpClient.newCall(request).execute() match {
      case r: Response if r.code() == 200 => resp = r
        processResponse(r)
      case _: Response => throw new IllegalStateException("There's fire on the mountain")
    }
  }

  var source: ResponseBody = _

  // Implementing the Server Sent Event Logic
  def processResponse(response: Response): Future[WatchObject] = {
    val buffer = new Buffer()
    source = response.body()
    val v = source.source()
    @volatile var wo: WatchObject = null

    // Returns true if there are no more bytes in this source. This will block until there are bytes
    // to read or the source is definitely exhausted.
    executeBlocking {
      breakable {
        while (!v.exhausted()) {
          v.read(buffer, 8192) match {
            case -1 =>
              cleanUpListener(v, buffer, response)
              throw new IllegalStateException("Source exhausted and object state is unknown")
            case _ =>
              wo = read[WatchObject](buffer.readUtf8())
              wo match {
                case WatchObject("DELETED", _) => cleanUpListener(v, buffer, response)
                case WatchObject(w, _) => println(s"${Thread.currentThread().getName} $w event still watching")
              }
          }
        }
      }
      wo
    }
  }

  def closeSource() = source.close()

  def cleanUpListener(source: BufferedSource, buffer: Buffer, response: Response): Unit = {
    source.close()
    buffer.close()
    break()
  }

  private def executeBlocking[T](cb: => T): Future[T] = {
    val p = Promise[T]()
    ec.execute(
      new Runnable {
        override def run(): Unit = {
          try {
            p.trySuccess(blocking(cb))
          } catch {
            case e: Throwable => println(s"${Thread.currentThread().getName} In here: ${e.getMessage}")
              p.tryFailure(e)
          }
        }
      })
    p.future
  }

}
