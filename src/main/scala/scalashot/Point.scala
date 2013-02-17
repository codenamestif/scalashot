package scalashot

sealed trait Point {
  val x: Int
  val y: Int
}

case class PressPoint(x: Int, y: Int) extends Point

case class DragPoint(x: Int, y: Int) extends Point

case class ReleasePoint(x: Int, y: Int) extends Point