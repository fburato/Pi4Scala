package pi4scala

class SimpleRequest[A](lb: LocalBuffer[A]) extends Request[A] {
  private var complete = false;
  def setVal(v: A): Boolean = {
    lb.set(v)
    true
  }

  def getVal(): (Boolean, A) = { (true, lb get) }

  def isComplete(): Boolean = { complete }

  def setComplete() =
    synchronized {
      complete = true
      notify()
    }

}
