package no.uio.musit

import org.scalatest.{MustMatchers, WordSpec}

class RoomSpec extends WordSpec with MustMatchers {

  "Room" should {

    "read single room with a unit" in {
      val csv = List(
        List("1", "unit 1")
      )
      val result = Room.apply(csv)

      result must have size 1
    }

    "read multiple rooms with a unit" in {
      val csv = List(
        List("1", "unit 1"),
        List("2", "unit 1")
      )
      val result = Room.apply(csv)

      result must have size 2
    }

    "read single room with multiple units" in {
      val csv = List(
        List("1", "unit 1"),
        List("1", "unit 2")
      )
      val result = Room.apply(csv)

      result must have size 1
      result.toList.head.storageUnit must contain allOf (
        StorageUnit(None, "unit 1", List.empty),
        StorageUnit(None, "unit 2", List.empty)
      )
    }

    "read single room with sub storage units " in {
      val csv = List(
        List("1", "unit 1", "unit 1-1"),
        List("1", "unit 2")
      )
      val result = Room.apply(csv)

      result must have size 1
      result.toList.head.storageUnit must contain
      StorageUnit(None, "unit 1", List(StorageUnit(None, "unit 1-1", List.empty)))
    }
  }
}
