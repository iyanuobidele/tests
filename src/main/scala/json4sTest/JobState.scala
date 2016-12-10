package json4sTest

/**
  * Created by iobidele on 12/5/2016.
  */

object JobState extends Enumeration {
  type JobState = Value

  /*
   * QUEUED - Spark Job has been queued to run
   * SUBMITTED - Driver Pod deployed but tasks are not yet scheduled on worker pod(s)
   * RUNNING - Task(s) have been allocated to worker pod(s) to run and Spark Job is now running
   * FINISHED - Spark Job ran and exited cleanly, i.e, worker pod(s) and driver pod were gracefully deleted
   * FAILED - Spark Job Failed due to error
   * KILLED - A user manually killed this Spark Job
   */
  val QUEUED, SUBMITTED, RUNNING, FINISHED, FAILED, KILLED = Value
}
