package pi4scala

import scala.concurrent.Lock

abstract class Channel[A] {
  def addReadRequest(r: Request[A])
  def addWriteRequest(r: Request[A])
  def removeRequest(r: Request[A])
  def <--(lb: LocalBuffer[A]) : A
  def getLock(): Lock
}
