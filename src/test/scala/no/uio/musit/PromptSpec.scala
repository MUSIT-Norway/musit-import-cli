package no.uio.musit

import org.scalatest.{MustMatchers, WordSpec}

class PromptSpec extends WordSpec with MustMatchers {

  "Prompt" should {
    "first input should be valid" in {
      val question = Question("key", "desc")
      val inputs = List("1")

      readAllInputAndVerifyLastInputIsReturned(question, inputs)
    }

    "read input until it's valid" in {
      val question = Question("key", "desc")
      val inputs = List("", "", "1")

      readAllInputAndVerifyLastInputIsReturned(question, inputs)
    }
  }

  def readAllInputAndVerifyLastInputIsReturned(
    question: Question,
    inputs: List[String]
  ) = {
    var values = inputs

    def reader(): String = {
      values match {
        case head :: Nil =>
          head
        case head :: trail =>
          values = trail
          head
        case _ =>
          throw new IllegalStateException("Invalid state")
      }
    }

    val input = Prompt.foldInput(None)(question, reader)

    input.value mustBe inputs.last
  }
}
