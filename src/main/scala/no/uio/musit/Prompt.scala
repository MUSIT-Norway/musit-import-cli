package no.uio.musit

import java.io.File

import scala.annotation.tailrec
import scala.io.StdIn

class Prompt(questions: Seq[Question]) {



  def ask(): Seq[Answer] = {
    implicit val readInput = () => StdIn.readLine
    questions.map(implicit q => {
      println(q.description)
      Prompt.foldInput(None)
    })
  }
}

object Prompt {
  @tailrec
  final def foldInput(
      state: Option[String],
      first: Boolean = true
  )(implicit q: Question, readInput: () => String): Answer = {
    if (first) {
      foldInput(Option(readInput()), first = false)
    } else if (state.isDefined && q.validator(state.getOrElse(""))) {
      Answer(q, state.get)
    } else {
      println("Doh! Not a valid input. Try again..")
      foldInput(Option(readInput()), first = false)
    }
  }
}
case class Question(
    key: String,
    description: String,
    validator: (String => Boolean) = Validators.NonEmpty
)

case class Answer(
    question: Question,
    value: String
)

object Validators {

  val NonEmpty = (input: String) => input.nonEmpty

  val FileExist = (input: String) => new File(input).exists()

}