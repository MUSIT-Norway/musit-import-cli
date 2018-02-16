package no.uio.musit

import play.api.libs.ws.ahc.AhcWSClient
import akka.actor.ActorSystem
import akka.event.slf4j.Logger
import akka.stream.ActorMaterializer
import com.github.tototoshi.csv.CSVReader
import no.uio.musit.csv.NorwegianCsvFormat

import scala.io.StdIn

class MainMoveObject()

object MainMoveObject extends App {
  implicit val actorSystem = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def getCsv(filename: String): List[List[String]] = {
    val reader = CSVReader.open(filename)(NorwegianCsvFormat)
    reader.all().map(_.filter(_.nonEmpty))
  }

  var logger = Logger(classOf[MainMoveObject], "musit")
  val wsClient = AhcWSClient()

  val inputParameters = getCsv("input_parameters.csv")
  inputParameters.head.zip(inputParameters.tail.head).map(x => println(s"${x._1}: ${x._2}"))
  val inputParametersData = inputParameters.tail.head

  val baseUrl = inputParametersData(0)
  val mid = inputParametersData(1).toLong
  val collection = inputParametersData(2)
  val token = inputParametersData(3)
  val mapFilename = inputParametersData(4)
  val regnoUuidMapFilename = inputParametersData(5)
  val startLocation = inputParametersData(6)
  val endBeforeLocation = inputParametersData(7)

  if (StdIn.readLine("Continue (y/n)? ") == "y") {

    val numLines1 = StdIn.readLine("Antall linjer: (Alle: [Enter]): ")
    val numLines = if (numLines1 != "") numLines1.toInt else -1

    logger.info(s"Get cached Object Uuids ($regnoUuidMapFilename)")

    val objectDataByCatalogNumberMap = getCsv(regnoUuidMapFilename)
      .filter(x => x.nonEmpty)
      .filter(x => x.length == 3)
      .map(x => (x(0), ObjectData(x(0), x(1), x(2)))).toMap

    logger.info(s"Get location name object catalog number map")

    val locationNameCatalogNumbers = getCsv(mapFilename)
      .tail
      .map(x => LocationNameCatalogNumber(x(0), x(1)))

    logger.info(s"locationNameCatalogNumbers: ${locationNameCatalogNumbers.length}")

    logger.info(s"Start location: $startLocation, End before location: $endBeforeLocation")

    val processedLocationNameCatalogNumbers1 = locationNameCatalogNumbers
      .dropWhile(x => x.locationName != startLocation)
      .takeWhile(x => x.locationName != endBeforeLocation)
    val processedLocationNameCatalogNumbers = if (numLines > 0) {
      processedLocationNameCatalogNumbers1.take(numLines)
    } else {
      processedLocationNameCatalogNumbers1
    }
    logger.info(s"objects processed: ${processedLocationNameCatalogNumbers.length}")

    val mover = new MoveObjectToLocation(
      wsClient,
      baseUrl,
      mid,
      collection,
      token,
      objectDataByCatalogNumberMap,
      processedLocationNameCatalogNumbers
    )

    var doStats: String = ""
    while ({ doStats = StdIn.readLine("Calculate statistics (y/n)? "); doStats == "" }) {}

    var doInfo: String = ""
    while ({ doInfo = StdIn.readLine("Collect info, (necessary for move) (y/n)? "); doInfo == "" }) {}

    var doMove: String = ""
    while ({ doMove = StdIn.readLine("Execute move (y/n)? "); doMove == "" }) {}

    val statsResult = if (doStats == "y") {
      val stats = mover.getCatalogNumberGroups()
      //stats.map(x => logger.info(s"${x._1} ${x._2.toString()} "))
      stats.map {
        case (s, l) =>
          logger.info(s"${s}: ${l.mkString(", ")} ")
      }
      Thread.sleep(1000)
    }

    val infoResult = if (doInfo == "y") {
      val info = mover.getLocationObjectInfo(processedLocationNameCatalogNumbers)
      val filteredLocations = info.filter(x => x.currentLocation == "-")
      logger.info("Objects to move")
      filteredLocations.map(x => logger.info(x.toString()))

      val result = if (doMove == "y") {
        mover.moveObjectsToLocation(filteredLocations)
      } else
        None
    }
    Thread.sleep(1000)
  }

  wsClient.close()
  actorSystem.terminate()
  logger.info("Finished, actorSystem terminated")

}

