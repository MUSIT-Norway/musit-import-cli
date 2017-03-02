package no.uio.musit

import play.api.libs.json.{JsNumber, JsObject, JsString}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

class StorageApi(
    wsClient: WSClient,
    baseUrl: String,
    mid: Long,
    token: String
)(implicit ec: ExecutionContext) {

  def insertStorageUnit(
    parent: Long,
    storageUnit: StorageUnit
  ): Future[Long] = {
    val d = JsObject(Seq(
      "type" -> JsString("StorageUnit"),
      "isPartOf" -> JsNumber(parent.toLong),
      "name" -> JsString(storageUnit.name)
    ))
    val endpoint = s"$baseUrl/v1/museum/$mid/storagenodes"
    wsClient.url(endpoint)
      .withHeaders("Authorization" -> token)
      .post(d)
      .map(r => r.status match {
        case 200 => (r.json \ "id").as[Long]
        case 201 => (r.json \ "id").as[Long]
        case 401 => throw new IllegalStateException(s"Not allowed to access $endpoint")
        case code => throw new IllegalStateException(
          s"http $code, endpoint: $endpoint, body: ${r.body}"
        )
      })
  }

}
