package no.uio.musit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient

import scala.util.{Failure, Success}

object Main extends App {

  val prompt = new Prompt(Seq(
    Question("endpoint", "Http endpoint to the environment:", Validators.Url),
    Question("token", "Session token for user. You have to copy it from browser:",
      Validators.Token),
    Question("file", "The input file path:", Validators.FileExist),
    Question("museum", "The museum id:", Validators.Number)
  ))

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val ac = ActorSystem()
  val client = AhcWSClient()(ActorMaterializer())

  val res = new ImportNodes(client)
    .doImport(prompt.ask())

  res match {
    case Success(f) =>
      f.onComplete {
        case Success(v) =>
          v.foreach(println)
          println("Done!")
          System.exit(0)
        case Failure(t) =>
          t.printStackTrace(System.out)
          println("Failed to execute inserts")
          System.exit(-1)
      }
    case Failure(t) =>
      t.printStackTrace(System.out)
      println("Failed while setting up stuff..")
      System.exit(-1)
  }

}
