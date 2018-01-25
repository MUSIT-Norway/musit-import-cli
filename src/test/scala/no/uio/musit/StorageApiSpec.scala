package no.uio.musit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.ws.ahc.AhcWSClient

import scala.concurrent.Await
//import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class StorageApiSpec extends WordSpec with MustMatchers {

  implicit val actorSystem = ActorSystem()
  implicit val mat = ActorMaterializer()
  val client = AhcWSClient()
  val endpoint = "http://musit-test:8888"
  val token = ""

  "StorageApi" should {
    pending
    "get storage location from valid name" in {

      val mid = 99L

      val storageApi = new StorageApi(client, endpoint, mid, token)

      Await.result(
        storageApi.getStorageUnitByName("Forskningsværelset"),
        Duration("10 seconds")
      ) mustBe "6e5b9810-9bbf-464a-a0b9-c27f6095ba0c"

    }
  }
}
