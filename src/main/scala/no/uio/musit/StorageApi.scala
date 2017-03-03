package no.uio.musit

import play.api.libs.json.{JsNumber, JsObject, JsString}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class StorageApi(
    wsClient: WSClient,
    baseUrl: String,
    mid: Long,
    token: String
) {

  def insertStorageUnit(
    node: Node
  ): Future[Long] = {
    node.parent match {
      case Some(id) =>
        val d = JsObject(Seq(
          "type" -> JsString(node.typ),
          "isPartOf" -> JsNumber(id),
          "name" -> JsString(node.name)
        ))
        val endpoint = s"$baseUrl/v1/museum/$mid/storagenodes"
        wsClient.url(endpoint)
          .withHeaders("Authorization" -> token)
          .withRequestTimeout(Duration.Inf)
          .post(d)
          .map(r => r.status match {
            case 200 => (r.json \ "id").as[Long]
            case 201 => (r.json \ "id").as[Long]
            case 401 =>
              throw new IllegalStateException(s"Not allowed to access $endpoint")
            case code =>
              throw new IllegalStateException(
                s"http $code, endpoint: $endpoint, body: ${r.body}"
              )
          })
      case None =>
        Future.failed(new IllegalArgumentException(
          s"Missing parent id for node with name ${node.name}"
        ))
    }

  }

}
