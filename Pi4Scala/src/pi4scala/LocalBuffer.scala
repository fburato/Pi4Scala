/**
 * File: LocalBuffer.scala
 * Package: pi4scala
 * Autore: Francesco Burato
 * Creazione: 11/apr/2013
 */
package pi4scala
/**
 * Provides implicts to convert from a value to a [[pi4scala.LocalBuffer]] and
 * vice-versa
 * 
 * @author Francesco Burato
 */
object LocalBuffer {
  /**
   * Converts from value to [[pi4scala.LocalBuffer]] containing that value
   * @param v the value to init the [[pi4scala.LocalBuffer]]
   * @return a [[pi4scala.LocalBuffer]] containing the given value
   */
  implicit def toLocalBuffer[A](v: A) = new LocalBuffer[A](v)
  /**
   * Converts from [[pi4scala.LocalBuffer]] to value getting its value
   * @param v the [[pi4scala.LocalBuffer]] from which get the value
   * @return the value of the [[pi4scala.LocalBuffer]]
   */
  implicit def toValueA[A](lb: LocalBuffer[A]): A = lb get
}
/**
 * Provides a buffer which [[pi4scala.Channel]] can access to 
 * satisfy their requests.
 * 
 * @author Francesco Burato
 * @constructor create a new local buffer with the given value
 * @param init the value which initialize the buffer
 */
class LocalBuffer[A](init: A) {
  /**
   * A [[pi4scala.WrapOperation]] which as a reference to the current buffer
   */
  abstract class WrapBuffer extends WrapOperation {
    val lb = LocalBuffer.this
  }
  // value of the buffer
  private var value = init
  /**
   * Returns the values memorized in the buffer
   * @return the value of the buffer
   */
  def get = value
  /**
   * Sets the current LocalBuffer to a given value
   * @param a the new value of the buffer
   */
  def set(a: A) = { value = a }
  /**
   * Performs a read from the given [[pi4scala.Channel]] to the current buffer.
   * @param chan a [[pi4scala.Channel]] which provides the value to be
   * read and written in the buffer
   * @return the value read from the channel and written in the buffer
   */
  private def read(chan: Channel[A]): A = {
    val sr = new SimpleRequest[A](this)
    // acquire atomic access to the channel
    chan.getLock.acquire
    try {
      // actually add the request to write
      chan.addReadRequest(sr)
    } finally {
      chan.getLock.release
    }
    sr.synchronized {
      while (!sr.isComplete) {
        // wait until the request has been served
        sr.wait()
      }
    }
    value
  }
  /**
   * Syntactic sugar for [[pi4scala.LocalBuffer.read]]
   */
  def <--(chan: Channel[A]) = read(chan)

  def <==(chan: Channel[A])(exe: => Unit): WrapOperation =
    new WrapBuffer {
      def update(sum: Choice) = {
        sum.addRead(lb, chan, () => exe)
      }
    }
  /**
   * Produce a [[pi4scala.WrapOperation]] which contains a request
   *  to read from the given channel and following which should make
   *  possible to execute a given function. Should be used to realizing
   *  nondeterministic input (or summation in Pi-calculus)
   *  @param chan the [[pi4scala.Channel]] containing the information to be read
   *  @param exe the unparametrized function to be executed in the event
   *  of actually performing the output operation
   *  @return a [[pi4scala.WrapOperation]] containing the write request
   *  and the closure of the given function
   *
   */
  def :=(v: A) = set(v)
  
  override def toString(): String = value.toString()
}
