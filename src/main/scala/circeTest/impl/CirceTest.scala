package circeTest.impl

import io.circe.generic.auto._
import io.circe.jawn.decode
import spray.http.{ContentTypes, HttpEntity}
import spray.httpx.unmarshalling.{Unmarshaller, _}
import spray.httpx.marshalling._

object CirceTest extends App {

  type ResourceList = Map[String, String]
  type ListMetadata = Map[String, String]

  case class Metadata(name: String,
                      uid: String = "",
                      labels: Option[Map[String, String]],
                      annotations: Option[Map[String, String]])

  case class ResourceRequirements(limits: Option[ResourceList], requests: Option[ResourceList])
  case class Container(name: String, resources: ResourceRequirements)
  case class PodSpecification(containers: List[Container])
  case class PodStatus(phase: String)
  case class Pod(status: PodStatus, metadata: Metadata, spec: PodSpecification)
  case class PodList(apiVersion: String, kind: String, metadata: ListMetadata, items: List[Pod])
  case class Event(apiVersion: Option[String],
                   count: Int,
                   firstTimestamp: String,
                   lastTimestamp: String,
                   involvedObject: ObjectReference,
                   kind: Option[String],
                   message: String,
                   metadata: Option[Metadata],
                   reason: String,
                   source: Option[EventSource],
                   `type`: String)

  case class EventSource(component: String, host: Option[String])

  case class ObjectReference(apiVersion: Option[String],
                             kind: String,
                             name: String,
                             namespace: String,
                             uid: String)
  //val sampleJson = ""

  //println(decode[PodList](sampleJson))
    val json =
      """{
        |"metadata": {
        |             "name":"k8s-scheduler-1401727561-ucifn.147e7b7b1d72f932",
        |             "namespace":"iobidele",
        |             "selfLink":"/api/v1/namespaces/iobidele/events/k8s-scheduler-1401727561-ucifn.147e7b7b1d72f932",
        |             "uid":"bf51dcb7-94d0-11e6-ba6f-fa163e7decbb",
        |             "resourceVersion":"4489761",
        |             "creationTimestamp":"2016-10-18T01:18:16Z"
        |             },
        |"involvedObject":{
        |             "kind":"Pod",
        |             "namespace":"iobidele",
        |             "name":"k8s-scheduler-1401727561-ucifn",
        |             "uid":"bf50186f-94d0-11e6-ba6f-fa163e7decbb",
        |             "apiVersion":"v1",
        |             "resourceVersion":"1456039"
        |             },
        |"reason":"Scheduled",
        |"message":"Successfully assigned k8s-scheduler-1401727561-ucifn to ccc-site-001-node-x0wdslza",
        |"source":{
        |           "component":"default-scheduler"
        |           },
        |"firstTimestamp":"2016-10-18T01:18:16Z",
        |"lastTimestamp":"2016-10-18T01:18:16Z",
        |"count":1,
        |"type":"Normal"
        |}
        |""".stripMargin

  println(decode[Event](json))
  //val entity =  HttpEntity(ContentTypes.`application/json`, sampleJson)

  //println(entity.as[PodList])
}
