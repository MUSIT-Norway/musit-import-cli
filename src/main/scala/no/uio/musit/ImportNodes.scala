package no.uio.musit

import java.io.File

import com.github.tototoshi.csv.CSVReader
import no.uio.musit.csv.NorwegianCsvFormat
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ImportNodes(client: WSClient)(implicit ec: ExecutionContext) {

  def doImport(questions: Seq[Answer]): Try[Future[Set[StorageUnit]]] = {
    def insertUnits(
      pId: Long,
      su: List[StorageUnit]
    )(implicit storageApi: StorageApi): Future[List[StorageUnit]] = {
      Future.sequence(su.map { u =>
        storageApi.insertStorageUnit(pId, u)
          .flatMap(id => {
            insertUnits(id, u.storageUnit)
              .map(isu => u.copy(id = Some(id), storageUnit = isu))
          })
      })
    }

    Try {
      val file = questions.find(_.question.key == "file").map(a => new File(a.value)).get
      val endpoint = questions.find(_.question.key == "endpoint").map(_.value).get
      val token = questions.find(_.question.key == "token").map(_.value).get
      val mid = questions.find(_.question.key == "museum").map(_.value.toLong).get

      val storageApi = new StorageApi(client, endpoint, mid, token)

      val reader = CSVReader.open(file)(NorwegianCsvFormat)
      (Room(reader.all()), storageApi)
    }.map {
      case (rooms, storageApi) =>
        implicit val sa = storageApi
        Future.sequence(rooms.map(r => insertUnits(r.id, r.storageUnit)))
          .map(_.flatten)
    }
  }

}

object Room {
  def apply(csv: List[List[String]]): Set[Room] = {

    def toUnits(lines: List[List[String]]): List[StorageUnit] = {
      lines.groupBy(_.head).mapValues(_.tail)
        .map {
          case (name, rest) =>
            StorageUnit(None, name, toUnits(rest))
        }
        .toList
    }

    csv.groupBy(_.head)
      .mapValues(_.map(_.tail))
      .map {
        case (room, units) => Room(room.toLong, toUnits(units))
      }
      .toSet
  }
}

case class Room(
  id: Long,
  storageUnit: List[StorageUnit]
)

case class StorageUnit(
  id: Option[Long],
  name: String,
  storageUnit: List[StorageUnit]
)