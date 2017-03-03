package no.uio.musit

import akka.actor.{Actor, Props}
import akka.event.slf4j.Logger
import no.uio.musit.CounterActor.{AddFailure, AddSuccess}

class CounterActor(expectedCount: Int) extends Actor {

  val logger = Logger(classOf[CounterActor], "musit")

  var success = 0
  var failure = 0
  var messages = 0

  override def receive = {
    case AddSuccess =>
      success = success + 1
      log()
    case AddFailure =>
      failure = failure + 1
      log()
    case _ =>
      messages = messages + 1
      log()
  }

  def log(): Unit = {
    if ((messages + success + failure) % 100 == 0) {
      logger.info(s"Counter: messsages: $messages success: $success failures: $failure")
    }
    if ((success + failure) == expectedCount) {
      logger.info(s"Done: messages $messages success: $success failures: $failure")
      context.system.terminate()
      System.exit(0)
    }
  }
}

object CounterActor {

  case object AddSuccess

  case object AddFailure

  def apply(expectedCount: Int): Props = Props(classOf[CounterActor], expectedCount)

}