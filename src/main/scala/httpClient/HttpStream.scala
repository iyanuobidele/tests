package httpClient

/**
  * Created by iyanu on 12/9/16.
  */

import java.io.IOException
import java.util.concurrent.TimeUnit

import json4sTest.JobState
import json4sTest.JobState._
import okhttp3.{Call, Callback, OkHttpClient, Request, Response}
import okio.{Buffer, BufferedSource}
import org.json4s.{CustomSerializer, DefaultFormats, JString}
import org.json4s.JsonAST.JNull
import org.json4s.jackson.Serialization.read

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

  private implicit val formats = DefaultFormats + JobStateSerDe
  private val kind = "SparkJob"
  private val apiVersion = "apache.io/v1"
  private val protocol = "http://"
  private val apiEndpoint = s"${protocol}127.0.0.1:8001/apis/$apiVersion/namespaces/iobidele/sparkjobs"
  private val httpClient = new OkHttpClient.Builder().readTimeout(0, TimeUnit.MILLISECONDS).build()

  val request = new Request.Builder().get().url(s"$apiEndpoint?watch=true").build()
  val response = httpClient.newCall(request).enqueue(new Callback {

    override def onFailure(call: Call, e: IOException): Unit = {
      throw new IllegalStateException(e.getMessage)
    }

    override def onResponse(call: Call, response: Response): Unit = {
      response.code() match {
        case 200 => processResponse(response)
        case _ => throw new IllegalStateException(s"Application layer failure. ${response.message()}")
      }
    }
  })


  // Implementing the Server Sent Event Logic
  def processResponse(response: Response): Unit = {
    val buffer = new Buffer()
    val source: BufferedSource = response.body().source()

    // Returns true if there are no more bytes in this source. This will block until there are bytes
    // to read or the source is definitely exhausted.
    while (!source.exhausted()) {
      source.read(buffer, 8192) match {
        case -1 => source.close()
        case _ =>
          val watchObject = read[WatchObject](buffer.readUtf8())
          processResponseUtil(watchObject)
      }
    }
  }

  def processResponseUtil(watchObject: WatchObject): Unit = {
    watchObject match {
      case WatchObject("DELETED", _) => println("I should go crazy")
      case WatchObject(_, _) => println("I don't really care")
    }
  }
}
