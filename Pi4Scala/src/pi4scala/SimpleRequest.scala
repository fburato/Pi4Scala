/**
 * File: SimpleRequest.scala
 * Package: pi4scala
 * Autore: Francesco Burato
 * Creazione: 11/apr/2013
 */
package pi4scala

/**
 * Provides the simplest [[pi4scala.Request]] available.
 * 
 * It simply possess a [[pi4scala.LocalBuffer]] in which read and
 * write operation are possible and wakes up the thread executing
 * the requests when completed.
 * 
 * @author Francesco Burato
 * 
 * @constructor create a new SimpleRequest with reference to the given
 * local buffer
 * 
 * @param lb a local buffer in which performing operations
 */
private[pi4scala] class SimpleRequest[A](lb: LocalBuffer[A]) extends Request[A] {
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
  /**
   * Returns the current Request as reference for synchronize
   */
  def getLock() = this
}
