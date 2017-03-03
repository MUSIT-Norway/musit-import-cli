package no.uio.musit

import org.scalatest.{MustMatchers, WordSpec}

class ImportNodesSpec extends WordSpec with MustMatchers {

  "Room" should {

    "read single room with a unit" in {
      val csv = List(
        List("1", "unit 1")
      )
      val (result, _) = ImportNodes.fromCsvToRoom(csv)

      result must have size 1
    }

    "read multiple rooms with a unit" in {
      val csv = List(
        List("1", "unit 1"),
        List("2", "unit 1")
      )
      val (result, _) = ImportNodes.fromCsvToRoom(csv)

      result must have size 2
    }

    "read single room with multiple units" in {
      val csv = List(
        List("1", "unit 1"),
        List("1", "unit 2")
      )
      val (result, _) = ImportNodes.fromCsvToRoom(csv)

      result must have size 1
      result.toList.head.children must contain allOf (
        StorageUnit("unit 1", Some(1), List.empty),
        StorageUnit("unit 2", Some(1), List.empty)
      )
    }

    "read single room with sub storage units " in {
      val csv = List(
        List("1", "unit 1", "unit 1-1"),
        List("1", "unit 2")
      )
      val (result, _) = ImportNodes.fromCsvToRoom(csv)

      result must have size 1
      result.toList.head.children must contain
      StorageUnit("unit 1", None, List(StorageUnit("unit 1-1", None, List.empty)))
    }

    "read single room with sub storage units on the same level" in {
      val csv = List(
        List("1", "1-1", "1-1-1"),
        List("1", "1-1", "1-1-2")
      )
      val (result, _) = ImportNodes.fromCsvToRoom(csv)

      result must have size 1
      val room = result.toList.head
      val firstNode = room.children.head
      firstNode.children must contain allOf (
        StorageUnit("1-1-1", None, List.empty),
        StorageUnit("1-1-2", None, List.empty)
      )
    }

    "count expected items to be inserted with sub nodes" in {
      val csv = List(
        List("1", "unit 1", "unit 1-1"),
        List("1", "unit 2")
      )
      val (_, count) = ImportNodes.fromCsvToRoom(csv)

      count mustBe 3
    }
  }
}
