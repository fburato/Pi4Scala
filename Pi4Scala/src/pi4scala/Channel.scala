package pi4scala

import scala.concurrent.Lock
/**
 * An interface to a general Channel which provides basic communication
 *  utility in Pi-calculus style
 */
abstract class Channel[A] {
  /**
   * Add to the current Channel a request to read from it.
   *  @param r the request to be added to the channel
   */
  def addReadRequest(r: Request[A])
  /**
   * Add to the current Channel a request to write in it
   * @param r the request to be added to the channel
   */
  def addWriteRequest(r: Request[A])
  /**
   * Ask to the current channel to remove any read or write request which
   *  references to a given request
   * @param r the request asked to remove from the channel
   */
  def removeRequest(r: Request[A])
  /**
   * Performs a write to the channel getting the information from the
   *  buffer given. The write on the channel is the Pi-calculus equivalent
   *  of an input to the channel so it blocks the thread performing the
   *  input until the request has been executed and completed.
   * @param lb the buffer containing the information to be input in the
   * channel
   * @return the information which was output in the the channel (for
   * eventual use
   *
   */
  def <--(lb: LocalBuffer[A]): A

  /**
   * Produce a [[pi4scala.WrapOperation]] which should contain a request
   *  to write in the current channel and following which should make
   *  possible to execute a given function. Should be used to realizing
   *  nondeterministic input (or summation in Pi-calculus)
   *  @param lb the buffer containing the information to be input in the
   *  channel
   *  @param exe the unparametrized function to be executed in the event
   *  of actually performing the output operation
   *  @return a [[pi4scala.WrapOperation]] containing the write request
   *  and the closure of the given function
   *
   */
  def <==(lb: LocalBuffer[A])(exe: => Unit): WrapOperation
  /**
   * Returns the lock used to access to the current channel
   *  @return a [[scala.concurrent.Lock]] usable to perform an atomic access
   *  to the channel
   */
  def getLock(): Lock
}
