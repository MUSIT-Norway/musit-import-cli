package no.uio.musit

import java.nio.file.Files

import org.scalatest.{MustMatchers, WordSpec}

class ValidatorsSpec extends WordSpec with MustMatchers {

  "NonEmpty validator" should {

    "be valid when not empty input" in {
      Validators.NonEmpty("not empty") mustBe true
    }

    "be invalid when empty input" in {
      Validators.NonEmpty("") mustBe false
    }

  }

  "FileExist validator" should {

    "be valid when file exist" in {
      val file = Files.createTempFile("existing_file", "txt")
      Validators.FileExist(file.toString) mustBe true
    }

    "be invalid when file doesn't exist" in {
      Validators.FileExist("./some_non_existing_file.txt") mustBe false
    }

  }

  "Token validator" should {

    "be valid token format" in {
      Validators.Token("Bearer 00000000-1111-2222-3333-444444444444") mustBe true
    }

    "be invalid token format" in {
      Validators.Token("00000000-1111-2222-3333-444444444444") mustBe false
    }

  }

  "Url validator" should {

    "be valid url format with http protocol" in {
      Validators.Url("http://localhost/api") mustBe true
    }

    "be valid url format with https protocol" in {
      Validators.Url("https://localhost/api") mustBe true
    }

    "be invalid url format when protocol is missing" in {
      Validators.Url("localhost/api") mustBe false
    }

  }

  "Number validator" should {

    "be valid int" in {
      Validators.Number("42") mustBe true
    }

    "be invalid when it's text" in {
      Validators.Number("tada") mustBe false
    }

  }
}
