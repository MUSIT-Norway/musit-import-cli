package no.uio.musit

import java.io.File

import akka.actor.{ActorRef, ActorSystem}
import akka.event.slf4j.Logger
import com.github.tototoshi.csv.CSVReader
import no.uio.musit.csv.NorwegianCsvFormat
import play.api.libs.ws.WSClient

import scala.util.Try

class ImportNodes(client: WSClient, as: ActorSystem) {

  var logger = Logger(classOf[ImportNodes], "musit")

  def doImport(questions: Seq[Answer]) = {
    Try {
      val file = questions.find(_.question.key == "file").map(a => new File(a.value)).get
      val endpoint = questions.find(_.question.key == "endpoint").map(_.value).get
      val token = questions.find(_.question.key == "token").map(_.value).get
      val mid = questions.find(_.question.key == "museum").map(_.value.toLong).get

      val storageApi = new StorageApi(client, endpoint, mid, token)

      val reader = CSVReader.open(file)(NorwegianCsvFormat)
      val lines = reader.all().map(_.filter(_.nonEmpty))
      val (rooms, expectedInserts) = ImportNodes.fromCsvToRoom(lines)
      (rooms, storageApi, expectedInserts)
    }.map {
      case (rooms, storageApi, totalInserts) =>
        implicit val sa = storageApi

        logger.info(s"Starting import of $totalInserts nodes")
        val addNodeActor = as.actorOf(AddNodeActor.apply(storageApi, totalInserts))

        rooms.foreach { room =>
          room.children.foreach { c =>
            addNodeActor.tell(c, ActorRef.noSender)
          }
        }
    }
  }
}

object ImportNodes {

  def fromCsvToRoom(csv: List[List[String]]): (Set[Room], Int) = {
    def toUnits(
      parent: Option[Long],
      lines: List[List[String]]
    ): (List[StorageUnit], Int) = {
      val items = lines
        .filter(_.nonEmpty)
        .groupBy(_.head)
        .mapValues(_.map(_.tail))
        .map {
          case (name, rest) =>
            val (child, cCount) = toUnits(None, rest)
            (StorageUnit(name, parent, child), cCount)
        }
        .toList
      val count = items.foldLeft(items.size) { case (c, i) => c + i._2 }
      (items.map(_._1), count)
    }

    val rooms = csv.groupBy(_.head)
      .mapValues(_.map(_.tail))
      .map {
        case (room, units) =>
          val (su, count) = toUnits(Some(room.toLong), units)
          (Room("", None, su), count)
      }
      .toSet
    (rooms.map(_._1), rooms.foldLeft(0) { case (c, i) => c + i._2 })
  }
}
