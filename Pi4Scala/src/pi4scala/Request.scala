/**
 * File: Request.scala
 * Package: pi4scala
 * Autore: Francesco Burato
 * Creazione: 11/apr/2013
 */
package pi4scala
/**
 * An interface to a general Request which make possible comunication between
 * threads using [[pi4scala.Channel]]s
 * 
 * @author Francesco Burato
 */
abstract class Request[A] {
  /**
   * Permits access to the current Request to write in it
   * @param v a value to be written in the current Request
   * @return true iff the operation has success
   */
  def setVal(v :A) :Boolean
  /**
   * Permits access to the current Request to read from it
   * @return (true,currently hold value) iff the operation has success
   */
  def getVal() : (Boolean,A)
  /**
   * Returns whether or not the request has been already served
   * @return true iff the request has been completed
   */
  def isComplete() : Boolean
  /**
   * Set the current Request to be completed.
   */
  def setComplete()
  /**
   * Returns a reference usable to perform atomic access to the Request.
   * It may not be the Request itself.
   * @return a reference to be used in a synchronized block
   */
  def getLock(): AnyRef
}