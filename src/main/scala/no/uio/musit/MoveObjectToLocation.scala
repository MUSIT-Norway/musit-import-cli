package no.uio.musit

import akka.event.slf4j.Logger
import com.github.tototoshi.csv.CSVReader
import no.uio.musit.MainMoveObject.res
import no.uio.musit.csv.NorwegianCsvFormat
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class ObjectData2(box: String, catalogNumber: String, regno: String)
case class ObjectMoveLocation(
  box: String, catalogNumber: String, objUuid: String, dbCatalogNumber: String, locId: String, curLoc: String
)
class MoveObjectToLocation(
    wsClient: WSClient,
    baseUrl: String,
    mapFilename: String,
    mid: Long,
    collection: String,
    token: String
) {

  val storageApi = new StorageApi(wsClient, baseUrl, mid, token)

  var logger = Logger(classOf[MoveObjectToLocation], "musit")

  def moveObjects(): Future[List[ObjectMoveLocation]] = {

    val mapping = getMoveMappingFromCsv(mapFilename)
      .tail.map(x => (x(0), x(1)))

    val res = mapping.filter(x => x._2.toInt < 1050).map {
      //val res = mapping.map {
      case (box: String, museumNo: String) => {
        logger.info(s"$box, $museumNo")
        val r = for {
          obData <- getObjectDataFromCatalogNumber(museumNo)
          dbObjName <- getObjNameByUUID(obData.uuid, collection)
          loc <- storageApi.getStorageUnitByName(box)
          curLoc <- getCurrentLocationByObjectUiid(obData.uuid)
        } yield { ObjectMoveLocation(box, museumNo, obData.uuid, dbObjName, loc, curLoc.getOrElse("-")) }
        r.onComplete {
          case Success(s) => logger.info(s"moveObjects: box: ${s.box}, catalogNumber: ${s.catalogNumber}, objUuid: ${s.objUuid}, dbCatalogNumber: ${s.dbCatalogNumber}, locId: ${s.locId}, curLoc: ${s.curLoc}")
          case Failure(s) => logger.info(s"moveObjects: Failed: ${s.getMessage()}")
        }
        Await.result(r, Duration.Inf)
        r
      }
    }

    logger.trace("moveObjects: før future seq")
    val v = Future.sequence(res) //.map {x => Future.sequence(x.)}.map {x => x}
    logger.trace("moveObjects: etter future seq")
    v.onComplete {
      case Success(s) => logger.trace("moveObjects: future seq success")
      case Failure(s) => logger.trace("moveObjects: future seq failure:" + s)
    }
    v
  }

  def getMoveMappingFromCsv(filename: String): List[List[String]] = {
    val reader = CSVReader.open(filename)(NorwegianCsvFormat)
    reader.all().map(_.filter(_.nonEmpty))
  }

  case class ObjectData(
    catalogNumber: String,
    uuid: String
  )

  case class ObjectNodeLocation(objectUuid: String, storageLocationId: String)

  def getObjectDataFromCatalogNumber(catalogNumberNum: String): Future[ObjectData] = {

    logger.trace(s"getObjectDataFromCatalogNumber: getting $catalogNumberNum")

    val endpoint =
      s"$baseUrl/api/thingaggregate/museum/$mid/objects/search"

    val req = wsClient
      .url(endpoint)
      .withHeaders("Authorization" -> token)
      .withQueryString(
        "collectionIds" -> collection,
        "museumNo" -> catalogNumberNum,
        "from" -> "0",
        "limit" -> "100"
      )
      .withRequestTimeout(30.second)

    val res = req
      .get()

    val res2 = res
      .map(r => {
        //        logger.trace(s"Object: $catalogNumberNum, status: ${r.status}")
        val v = r.status match {
          case 200 => {
            logger.trace(s"getObjectDataFromCatalogNumber:  ($catalogNumberNum) status 200")

            val total = (r.json \ "hits" \ "total").as[Int]
            if (total != 1) throw new IllegalStateException(s"ES hits total != 1")

            val entries = (r.json \ "hits" \ "hits").as[JsArray].value
            //            logger.trace(s"entries: $entries")
            val objUuid = (entries.head \ "_source" \ "id").as[String]
            val museumNo = (entries.head \ "_source" \ "museumNo").as[String]

            ObjectData(museumNo, objUuid)
          }
          case 401 =>
            throw new IllegalStateException(s"Not allowed to access $endpoint")
          case code =>
            throw new IllegalStateException(
              s"http $code, endpoint: $endpoint, body: ${r.body}, r: ${r.allHeaders.toString()}"
            )
        }
        logger.trace("getObjectDataFromCatalogNumber: done getObjectByRegno")
        v
      })
    res2
      .onComplete {
        case Success(s) => logger.trace(s"getObjectDataFromCatalogNumber: success ${s.catalogNumber}")
        case Failure(f) => logger.trace(s"getObjectDataFromCatalogNumber: failure ${f.getMessage}")
      }
    res2

  }

  def moveObjectToStorageUnit(objLoc: ObjectNodeLocation): Future[Long] = {
    val endpoint =
      s"$baseUrl/api/storagefacility/museum/$mid/storagenodes/moveObject"

    logger.trace(s"moveObjectToStorageUnit: mid: $mid, endpoint: $endpoint")

    //  "doneBy": "${adminId.asString}",
    val moveJson = Json.parse(
      s"""{
         |  "destination": "${objLoc.storageLocationId}",
         |  "items": [{
         |    "id": "${objLoc.objectUuid}",
         |    "objectType": "collection"
         |  }]
         |}""".stripMargin
    )
    logger.trace(s"Move json: ${moveJson.toString()}")
    //return temporary dummy value instead of actually executing move.
    ???

    //    wsClient
    //      .url(endpoint)
    //      .withHeaders("Authorization" -> token)
    //      .put(moveJson)
    //      .map(r => {
    //        println(r.body)
    //        r.status match {
    //          case 200 => (r.json \ "id").as[Long]
    //          case 201 => (r.json \ "id").as[Long]
    //          case 401 =>
    //            throw new IllegalStateException(s"Not allowed to access $endpoint")
    //          case code =>
    //            throw new IllegalStateException(
    //              s"http $code, endpoint: $endpoint, body: ${r.body}"
    //            )
    //        }
    //      })

  }

  def getObjNameByUUID(objUUID: String, collection: String): Future[String] = {
    logger.trace(s"getObjNameByUUID, getting $objUUID")

    val endpoint =
      s"$baseUrl/api/thingaggregate/museum/$mid/objects/$objUUID"

    val req = wsClient
      .url(endpoint)
      .withHeaders("Authorization" -> token)
      .withQueryString(
        "collectionIds" -> collection
      )
      .withRequestTimeout(30.second)
    val res = req
      .get()
    res
      .map(r => {
        logger.trace(s"getObjNameByUUID: Object: $objUUID, status: ${r.status}")
        val v = r.status match {
          case 200 => {
            logger.trace(s"getObjNameByUUID: 200 regno ($objUUID) body: ${r.body}")
            //            val entries = (r.json \ "matches").as[JsArray].value
            //            logger.trace(s"entries: $entries")

            logger.trace(s"getObjNameByUUID: json: ${r.json.toString()}")

            //            val entries = (r.json \ "hits" \ "hits").as[JsArray].value
            val jsonUuid = (r.json \ "uuid").as[String]
            if (objUUID != jsonUuid) throw new IllegalStateException(s"UUID not matching")
            val museumNo = (r.json \ "museumNo").as[String]

            logger.trace(s"getObjNameByUUID: objUuid: $jsonUuid")
            museumNo
          }
          case 401 =>
            throw new IllegalStateException(s"Not allowed to access $endpoint")
          case code =>
            throw new IllegalStateException(
              s"http $code, endpoint: $endpoint, body: ${r.body}"
            )
        }
        logger.trace("getObjNameByUUID: done")
        v
      })

  }

  def getCurrentLocationByObjectUiid(objUUID: String): Future[Option[String]] = {
    logger.trace(s"getCurrentLocationByObjectUiid, getting $objUUID")

    val endpoint =
      s"$baseUrl/api/storagefacility/museum/$mid/storagenodes/objects/$objUUID/currentlocation"

    val req = wsClient
      .url(endpoint)
      .withHeaders("Authorization" -> token)
      .withQueryString(
        "collectionIds" -> collection
      )
      .withRequestTimeout(30.second)
    val res = req
      .get()
    res
      .map(r => {
        logger.trace(s"Object: $objUUID, status: ${r.status}")
        val v = r.status match {
          case 200 => {
            logger.trace(s"getCurrentLocationByObjectUiid: 200 regno ($objUUID) body: ${r.body}")

            logger.trace(s"json: ${r.json.toString()}")

            val nodeId = (r.json \ "nodeId").as[String]

            logger.trace(s"nodeId: $nodeId")
            Some(nodeId)
          }
          case 401 =>
            throw new IllegalStateException(s"Not allowed to access $endpoint")
          case 404 =>
            //            throw new IllegalStateException(s"Object not found")
            None
          case code =>
            throw new IllegalStateException(
              s"http $code, endpoint: $endpoint, body: ${r.body}"
            )
        }
        logger.trace("getCurrentLocationByObjectUiid: done")
        v
      })

  }

}

