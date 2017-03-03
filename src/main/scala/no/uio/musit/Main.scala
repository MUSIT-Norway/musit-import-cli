package no.uio.musit

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.ws.ahc.AhcWSClient

object Main extends App {

  val prompt = new Prompt(Seq(
    Question("endpoint", "Http endpoint to the environment:", Validators.Url),
    Question("token", "Session token for user. You have to copy it from browser:",
      Validators.Token),
    Question("file", "The input file path:", Validators.FileExist),
    Question("museum", "The museum id:", Validators.Number)
  ))

  implicit val actorSystem = ActorSystem()
  implicit val mat = ActorMaterializer()
  val client = AhcWSClient()

  val res = new ImportNodes(client, actorSystem)
    .doImport(prompt.ask())

}
