package no.uio.musit

import play.api.libs.ws.ahc.AhcWSClient
import akka.actor.ActorSystem
import akka.event.slf4j.Logger
import akka.stream.ActorMaterializer
import no.uio.musit.MainMoveObject.logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

object Test {

  def main(args: Array[String]) {
    var x = 0
    def first(): Unit = {

      println("startFirst");
      Thread.sleep(3000); println("stopFirst")
      x = x + 1
    }

    def second(): Unit = {
      println("startSecond");
      Thread.sleep(1000);
      println("stopSecond");
      throw new IllegalStateException("hallo feilen")
    }
    val producer = {
      val list = Seq(
        Future(5).map(_ => first()),
        Future(5).map(_ => second())

      )
      Future.sequence(list)
    }
    producer.onComplete(_ => println(s"Ferdig: $x"))

    Thread.sleep(5000)
    //Await.result(producer, Duration.Inf)
  }
}

class MainMoveObject()

object MainMoveObject extends App {
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  var logger = Logger(classOf[MainMoveObject], "musit")

  val wsClient = AhcWSClient()
  val baseUrl = "https://musit-utv.uio.no"
  val mid = 4L
  val collection = "7352794d-4973-447b-b84e-2635cafe910a" //karplante
  val token = "Bearer " //"Bearer ..."

  //  val mapFilename = "/home/sveigl/Documents/Musit/2017/nhm-magasinnoder/import_regnr_magasin_dev3.csv"
  val mapFilename = "/home/sveigl/Documents/Musit/2017/nhm-magasinnoder/imp_reg_mag_2col_n1000.csv"

  //  val mapFilename = "/home/sveigl/Documents/Musit/2017/nhm-magasinnoder/imp_dev_1001.csv"
  //val mapFilename = "/home/sveigl/Documents/Musit/2017/nhm-magasinnoder/imp_dev_1001_2.csv"
  //  val mapFilename = "/home/sveigl/Documents/Musit/2017/nhm-magasinnoder/imp_dev_255.csv"

  val mover = new MoveObjectToLocation(
    wsClient,
    baseUrl,
    mapFilename,
    mid,
    collection,
    token
  )

  val res = mover.moveObjects()

  res.onComplete { x =>

    logger.info("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
    logger.info("Finished, onComplete, shoud terminate actorSystem, but...")
    logger.info("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
  }
  logger.info("Start Await")
  Await.result(res, Duration.Inf)
  logger.info("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
  logger.info("Finished, terminating actorSystem")
  logger.info("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
  wsClient.close()
  actorSystem.terminate()
  Thread.sleep(1000)
  logger.info("Finished, actorSystem terminated")

}
