package no.uio.musit

sealed trait Node {
  val name: String
  val parent: Option[Long]
  val children: Seq[Node]

  def withParentId(id: Long): Node = this match {
    case r: Room => r.copy(parent = Some(id))
    case u: StorageUnit => u.copy(parent = Some(id))
  }

  def typ = this match {
    case _: Room => "Room"
    case _: StorageUnit => "StorageUnit"
  }
}

case class Room(
  name: String,
  parent: Option[Long],
  children: Seq[Node]
) extends Node

case class StorageUnit(
  name: String,
  parent: Option[Long],
  children: Seq[Node]
) extends Node
