package json4sTest

/**
  * Created by iobidele on 12/5/2016.
  */
object MainTest extends App {
  val js = new Json4sTest

  val keyValuePairs = Map(
    "num-executors" -> 10,
    "image" -> "sparkImage",
    "state" -> JobState.SUBMITTED
  )

  js.createJobObject("podDriver", keyValuePairs)

  js.updateJobObject("podDriver", "Test", "/spec/pod/")
}
