package no.uio.musit

import akka.event.slf4j.Logger
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class ObjectMoveLocation(
  locationName: String,
  catalogNumber: String,
  objectUuid: String,
  catalogNumberFull: String,
  storageLocationUuid: String,
  currentLocation: String
)

case class ObjectData(
  catalogNumber: String,
  CatalogNumberFull: String,
  objectUuid: String
)

case class LocationNameCatalogNumber(
  locationName: String,
  catalogNumber: String
)

case class LocationUuidObjectUuids(
  storageLocationUuid: String,
  objectUuids: Seq[String]
)

class MoveObjectToLocation(
    wsClient: WSClient,
    baseUrl: String,
    mid: Long,
    collection: String,
    token: String,
    objectDataByCatalogNumberMap: Map[String, ObjectData],
    locationNameCatalogNumbers: List[LocationNameCatalogNumber]
) {

  val storageApi = new StorageApi(wsClient, baseUrl, mid, token)

  var logger = Logger(classOf[MoveObjectToLocation], "musit")

  def getLocationUuid(locName: String): String = {
    logger.trace("getLocationUuid")
    val locUuid = storageApi.getStorageUnitByName(locName)
    logger.trace("getLocationUuid: Await...")
    Await.result(locUuid, Duration.Inf)
  }

  def getCurLocByObjUuid(objectUuid: String): Option[String] = {
    logger.trace("getCurLocByObjUuid")
    val locUuid = getCurrentLocationByObjectUiid(objectUuid)
    logger.trace("getCurLocByObjUuid: Await...")
    Await.result(locUuid, Duration.Inf)
  }

  def getLocationUuidMap(
    locationNameCatalogNumbers: List[LocationNameCatalogNumber]
  ): Map[String, String] = {
    logger.trace("getLocationUuidMap")

    val locationNames = locationNameCatalogNumbers.map(x => x.locationName).distinct
    val l = (locationNames.length / 5) + 1
    val locationNamesBatches = locationNames.grouped(l).toList

    val locationUuidsMap = locationNamesBatches.map { batch =>
      logger.trace(s"getLocationUuidMap: batch length: ${batch.length}")
      val v = batch.foldRight(List[(String, String)]()) { (locName, z) =>
        val locUuid = getLocationUuid(locName)
        logger.trace(s"got locationUuid $locUuid for location name $locName")
        (locName, locUuid) :: z
      }
      v
    }.foldRight(List[(String, String)]()) { (x, z) =>
      x ::: z
    }.toMap
    locationUuidsMap
  }

  def getCurrentLocationByCatalogNumberMap(
    locationNameCatalogNumbers: List[LocationNameCatalogNumber]
  ): Map[String, String] = {
    logger.trace("getCurrentLocationByCatalogNumberMap")
    val vv = locationNameCatalogNumbers.foldLeft(List[(String, String)]()) { (z, x) =>
      val objUuid = objectDataByCatalogNumberMap(x.catalogNumber).objectUuid
      val curLocF = getCurrentLocationByObjectUiid(objUuid)
      val curLocO = Await.result(curLocF, Duration.Inf)
      val curLoc = curLocO.getOrElse("-")
      logger.info(s"Got current location $curLoc for object ${x.catalogNumber}")
      (x.catalogNumber, curLoc) :: z
    }
    vv.toMap
  }

  logger.info("Create location Uuid map")
  val locationUuidMap = getLocationUuidMap(locationNameCatalogNumbers)

  logger.info("Create current location map")
  val curLocMap = getCurrentLocationByCatalogNumberMap(locationNameCatalogNumbers)

  def getLocationObjectInfo(
    locationNameCatalogNumbers: List[LocationNameCatalogNumber]
  ): List[ObjectMoveLocation] = {

    val res = locationNameCatalogNumbers.map { x =>
      val objData = objectDataByCatalogNumberMap(x.catalogNumber)
      val locUuid = locationUuidMap(x.locationName)
      val curLoc = curLocMap(x.catalogNumber)

      val v = ObjectMoveLocation(
        x.locationName,
        x.catalogNumber,
        objData.objectUuid,
        objData.CatalogNumberFull,
        locUuid,
        curLoc
      )
      logger.info(v.toString())
      //      println(v.toString())
      v
    }
    res
  }

  def moveObjectsToLocation(
    objectMoveLocations: List[ObjectMoveLocation]
  ): List[JsValue] = {
    logger.info("Prepare moving objects")
    //    logger.trace("ungrouped list")
    //    objectMoveLocations.map(x => logger.trace(s"${x.toString}"))
    val objectMoveLocationsMap = objectMoveLocations
      .groupBy(x => x.locationName)

    val orderedLocations = objectMoveLocations.map(x => x.locationName).distinct
    logger.trace(s"distinct: ${orderedLocations.toString()}")
    val orderedList = orderedLocations.map { locName =>
      val locs = objectMoveLocationsMap(locName).groupBy(y => y.storageLocationUuid)
      locs.map(x => x._2.map(y => logger.info(y.toString)))
      locs
    }

    val res = orderedList.map { locmap =>
      val r = locmap.toList.map {
        case (loc, objectMoveLocations) =>
          val locUuids =
            LocationUuidObjectUuids(loc, objectMoveLocations.map(x => x.objectUuid))

          val v = Await.result(moveObjectToStorageUnit(locUuids), Duration.Inf)
          val locname = objectMoveLocations.map(x => x.locationName).distinct.mkString
          logger.info(s"Moved to $locname")
          logger.info(s"Move result: $v")
          v
      }
      r
    }.flatten
    res
  }

  def getEsObjectDataFromCatalogNumber(catalogNumber: String): Future[ObjectData] = {

    logger.trace(s"getObjectDataFromCatalogNumber: getting $catalogNumber")

    val endpoint =
      s"$baseUrl/api/thingaggregate/museum/$mid/objects/search"

    val req = wsClient
      .url(endpoint)
      .withHeaders("Authorization" -> token)
      .withQueryString(
        "collectionIds" -> collection,
        "museumNo" -> catalogNumber,
        "from" -> "0",
        "limit" -> "100"
      )
      .withRequestTimeout(Duration.Inf)

    val res = req
      .get()

    val res2 = res
      .map(r => {
        //        logger.trace(s"Object: $catalogNumberNum, status: ${r.status}")
        val v = r.status match {
          case 200 => {
            logger.trace(s"getObjectDataFromCatalogNumber:  ($catalogNumber) status 200")

            val total = (r.json \ "hits" \ "total").as[Int]
            if (total != 1) throw new IllegalStateException(s"ES hits total != 1")

            val entries = (r.json \ "hits" \ "hits").as[JsArray].value
            //            logger.trace(s"entries: $entries")
            val esUuid = (entries.head \ "_source" \ "id").as[String]
            val esCatalogNumber = (entries.head \ "_source" \ "museumNo").as[String]
            logger.trace(s"getObjectDataFromCatalogNumber: ObjectData($catalogNumber, $esCatalogNumber, $esUuid)")
            ObjectData(catalogNumber, esCatalogNumber, esUuid)
          }
          case 401 =>
            throw new IllegalStateException(s"Not allowed to access $endpoint")
          case code =>
            throw new IllegalStateException(
              s"http $code, endpoint: $endpoint, body: ${r.body}, r: ${r.allHeaders.toString()}"
            )
        }
        v
      })
    res2
      .onComplete {
        case Success(s) => logger.trace(s"getObjectDataFromCatalogNumber: success ${s.catalogNumber}")
        case Failure(f) => logger.trace(s"getObjectDataFromCatalogNumber: failure ${f.getMessage}")
      }
    res2
  }

  def moveObjectToStorageUnit(objLoc: LocationUuidObjectUuids): Future[JsValue] = {
    val endpoint =
      s"$baseUrl/api/storagefacility/museum/$mid/storagenodes/moveObject"

    logger.trace(s"moveObjectToStorageUnit: mid: $mid, endpoint: $endpoint")
    val jsObjects = objLoc.objectUuids.map { objUuid =>
      Json.obj(
        "id" -> objUuid,
        "objectType" -> "collection"
      )
    }

    val moveJson = Json.obj(
      "destination" -> s"${objLoc.storageLocationUuid}",
      "items" -> jsObjects
    )
    logger.trace(s"Move json: ${moveJson.toString()}")
    wsClient
      .url(endpoint)
      .withHeaders("Authorization" -> token)
      .put(moveJson)
      .map(r => {
        val v = r.status match {
          case 200 => (r.json)
            .as[JsValue]

          case 401 =>
            throw new IllegalStateException(s"Not allowed to access $endpoint")
          case code =>
            throw new IllegalStateException(
              s"http $code, endpoint: $endpoint, body: ${r.body}"
            )
        }
        v
      })

  }

  def getObjectNameByUUID(objUUID: String, collection: String): Future[String] = {
    logger.trace(s"getObjNameByUUID, getting $objUUID")

    val endpoint =
      s"$baseUrl/api/thingaggregate/museum/$mid/objects/$objUUID"

    val req = wsClient
      .url(endpoint)
      .withHeaders("Authorization" -> token)
      .withQueryString(
        "collectionIds" -> collection
      )
      .withRequestTimeout(Duration.Inf)
    val res = req
      .get()
    res
      .map(r => {
        logger.trace(s"getObjNameByUUID: Object: $objUUID, status: ${r.status}")
        val v = r.status match {
          case 200 => {
            logger.trace(s"getObjNameByUUID: 200 regno ($objUUID) body: ${r.body}")
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
      .withRequestTimeout(Duration.Inf)
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

            logger.trace(s"getCurrentLocationByObjectUiid: found nodeId: $nodeId")
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

