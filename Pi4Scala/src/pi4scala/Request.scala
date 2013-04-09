package pi4scala

abstract class Request[A] {
  def setVal(v :A) :Boolean
  def getVal() : (Boolean,A)
  def isComplete() : Boolean
  def setComplete()
  def getLock(): AnyRef
}