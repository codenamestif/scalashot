package scalashot

sealed trait Action

case class SaveAction() extends Action

case class UploadAction() extends Action
