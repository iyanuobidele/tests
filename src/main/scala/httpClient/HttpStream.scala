package httpClient

/**
  * Created by iyanu on 12/9/16.
  */

import java.io.IOException

import json4sTest.JobState
import json4sTest.JobState._
import okhttp3.{Request, OkHttpClient, Response, Callback, Call}
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
  private val httpClient = new OkHttpClient()

  val request = new Request.Builder().get().url(s"$apiEndpoint?watch=true").build()
  val response = httpClient.newCall(request).enqueue(new Callback {
    override def onFailure(call: Call, e: IOException): Unit = {
      throw new IllegalStateException(e.getMessage)
    }

    override def onResponse(call: Call, response: Response): Unit = {
      if (response.code() == 200) {
        println("Successfully watching resource")
        println(read[WatchObject](response.body().charStream()))
      } else {
        val msg = s"Failed to watch resource. ${response.message()}"
        println(msg)
        throw new IllegalStateException(msg)
      }
    }
  })

}
