package no.uio.musit

import akka.event.slf4j.Logger
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class StorageApi(
    wsClient: WSClient,
    baseUrl: String,
    mid: Long,
    token: String
) {

  var logger = Logger(classOf[StorageApi], "musit")

  def insertStorageUnit(
    node: Node
  ): Future[Long] = {
    logger.trace(s"insertStorageUnit")

    node.parent match {
      case Some(id) =>
        val d = JsObject(
          Seq(
            "type" -> JsString(node.typ),
            "isPartOf" -> JsNumber(id),
            "name" -> JsString(node.name)
          )
        )
        val endpoint = s"$baseUrl/api/storagefacility/museum/$mid/storagenodes"
        wsClient
          .url(endpoint)
          .withHeaders("Authorization" -> token)
          .withRequestTimeout(Duration.Inf)
          .post(d)
          .map(r =>
            r.status match {
              case 200 => (r.json \ "id").as[Long]
              case 201 => (r.json \ "id").as[Long]
              case 401 =>
                throw new IllegalStateException(
                  s"Not allowed to access $endpoint"
                )
              case code =>
                throw new IllegalStateException(
                  s"http $code, endpoint: $endpoint, body: ${r.body}"
                )
            })
      case None =>
        Future.failed(
          new IllegalArgumentException(
            s"Missing parent id for node with name ${node.name}"
          )
        )
    }

  }

  def getStorageUnitByName(
    loc: String
  ): Future[String] = {
    logger.trace(s"getStorageUnitByName, getting $loc")

    val endpoint =
      s"$baseUrl/api/storagefacility/museum/$mid/storagenodes/search"

    val req = wsClient
      .url(endpoint)
      .withHeaders("Authorization" -> token)
      .withQueryString("searchStr" -> s"$loc")
      .withRequestTimeout(Duration.Inf)
    val res = req
      .get()
    res
      .map(r => {
        logger.trace(s"Location: $loc, status: ${r.status}")
        val v = r.status match {
          case 200 => {
            logger.trace(s"200($loc) body1: ${r.body}")

            val jsonSeq = r.json.as[Seq[JsObject]]

            if (jsonSeq.length > 1) {
              logger.error(s"Location name not unique: $loc, endpoint: $endpoint, body: ${r.body}")
              throw new IllegalStateException(
                s"Location name not unique: $loc, endpoint: $endpoint, body: ${r.body}"
              )
            }
            if (jsonSeq.length == 0) {
              logger.error(s"Location name not found: $loc, endpoint: $endpoint, body: ${r.body}")
              throw new IllegalStateException(
                s"Location name not found: $loc, endpoint: $endpoint, body: ${r.body}"
              )
            }
            val jsObj = jsonSeq.head
            val nodeId = (jsObj \ "nodeId").as[String]
            nodeId
          }
          case 401 =>
            throw new IllegalStateException(s"Not allowed to access $endpoint")
          case code =>
            throw new IllegalStateException(
              s"http $code, endpoint: $endpoint, body: ${r.body}"
            )
        }
        logger.trace("done getStorageUnitByName")
        v
      })

  }

}
