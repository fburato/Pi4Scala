package pi4scala

abstract class Channel[A] {
  def addReadRequest(r: Request[A]): Boolean
  def addWriteRequest(r: Request[A]): Boolean
  def removeRequest(r: Request[A])
  def <--(lb: LocalBuffer[A]) : A
}
