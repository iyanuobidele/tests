package json4sTest

import json4sTest.JobState.JobState
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization._

/**
  * Created by iobidele on 12/5/2016.
  */


object Json4sTest {

  case class Metadata(name: String, uid: Option[String] = None,
                      labels: Option[Map[String, String]] = None,
                      annotations: Option[Map[String, String]] = None)

  case class SparkJobState(apiVersion: String, kind: String,
                           metadata: Metadata, spec: Map[String, Any])

  case object JobStateSerDe extends CustomSerializer[JobState](_ =>
    ( {
      case JString("SUBMITTED") => JobState.SUBMITTED
      case JString("QUEUED") => JobState.QUEUED
      case JString("RUNNING") => JobState.RUNNING
      case JString("FINISHED") => JobState.FINISHED
      case JString("KILLED") => JobState.KILLED
      case JString("FAILED") => JobState.FAILED
      case JNull => throw new UnsupportedOperationException("No JobState Specified")
    }, {
      case JobState.FAILED => JString("FAILED")
      case JobState.SUBMITTED => JString("SUBMITTED")
      case JobState.KILLED => JString("KILLED")
      case JobState.FINISHED => JString("FINISHED")
      case JobState.QUEUED => JString("QUEUED")
      case JobState.RUNNING => JString("RUNNING")
    })
  )
}


class Json4sTest {

  import Json4sTest._

  implicit private val formats = DefaultFormats + JobStateSerDe
  private val kind = "SparkJob"
  private val apiVersion = "apache.io/v1"
  private val apiEndpoint = s"master/apis/$apiVersion/namespaces/iobidele/sparkjobs"
  var temp:String = _

  def createJobObject(name: String, keyValuePairs: Map[String, Any]): Unit = {
    val resourceObject = SparkJobState(apiVersion, kind, Metadata(name), keyValuePairs)
    println(keyValuePairs)
    val payload = parse(write(resourceObject))
    temp = pretty(render(payload))
    println(temp)
  }

  def updateJobObject(name: String, value: String, fieldPath: String): Unit = {
    val payload = List(("op" -> "replace") ~ ("path" -> fieldPath) ~ ("value" -> value))
    println(pretty(render(payload)))
    println(read[SparkJobState](temp))
  }

}
