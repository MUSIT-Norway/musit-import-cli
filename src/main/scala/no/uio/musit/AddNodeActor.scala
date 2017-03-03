package no.uio.musit

import akka.actor.{Actor, Props, Stash}
import akka.event.slf4j.Logger
import no.uio.musit.CounterActor.{AddFailure, AddSuccess}

import scala.concurrent.ExecutionContext.Implicits.global

class AddNodeActor(storageApi: StorageApi, expectedCount: Int) extends Actor with Stash {
  val logger = Logger(classOf[AddNodeActor], "musit")

  val activeRequest = new AddNodeActor.ActiveRequest
  val counter = context.actorOf(CounterActor(expectedCount))

  override def receive = {
    case AddNodeActor.Reduce =>
      activeRequest.dec()
      if (activeRequest.notCapped()) unstashAll()
    case node: Node if activeRequest.notCapped() =>
      activeRequest.inc()
      counter.tell(node, self)
      storageApi.insertStorageUnit(node)
        .map { id =>
          logger.debug(s"Inserted node ${node.typ} with ${node.name} to $id")
          node.children
            .map(_.withParentId(id))
            .foreach(c => self.tell(c, self))
          self.tell(AddNodeActor.Reduce, self)
          counter.tell(AddSuccess, self)
        }.onFailure {
          case t =>
            self.tell(AddNodeActor.Reduce, self)
            counter.tell(AddFailure, self)
            logger.warn(s"Failed to insert node ${node.typ}" +
              s" with ${node.name} parent: ${node.parent}", t)
        }
    case _: Node =>
      stash()
    case m => unhandled(m)
  }

}

object AddNodeActor {

  class ActiveRequest {
    var active = 0

    def inc() = active = active + 1

    def dec() = active = active - 1

    def capped() = active > 5

    def notCapped() = !capped()
  }

  case object Reduce

  def apply(storageApi: StorageApi, expectedCount: Int): Props =
    Props.apply(classOf[AddNodeActor], storageApi, expectedCount)

}