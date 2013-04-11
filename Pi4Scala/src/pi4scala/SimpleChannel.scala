/**
 * File: SimpleChannel.scala
 * Package: pi4scala
 * Autore: Francesco Burato
 * Creazione: 11/apr/2013
 */
package pi4scala

import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.concurrent.Lock

/**
 * Provides implicit for outputing from the channel without using a [[pi4scala.LocalBuffer]]
 * as exchange object
 * 
 * @author Francesco Burato
 */
object SimpleChannel {
  /**
   * Provides the default value usable to initialize in a correct way
   * a LocalBuffer
   */
  private def defaultValue[U]: U = { class Default[U] { var default: U = _ }; new Default[U].default }
  /**
   * Implicit method which perform an output from the channel and returns
   * just the value read from in
   * @param chan a [[pi4scala.Channel]] from which performing the output
   * @return the value read from the channel
   */
  implicit def <--[A](chan: Channel[A]): A = { new LocalBuffer[A](defaultValue[A]) <-- chan }
}

/**
 * A simple implementation of [[pi4scala.Channel]] which provides synchronous
 * read and write capabilities
 * 
 * @author Francesco Burato
 */
class SimpleChannel[A] extends Channel[A] {
  // array of request to read from the channel
  private val readRequest = new ArrayBuffer[Request[A]]
  // array of request to write in the channel
  private val writeRequest = new ArrayBuffer[Request[A]]
  // pseudorandom number generator to randomize access
  private val gen = new Random()
  // lock for using the current channel
  val lock = new ReentrantLock

  /**
   * A [[pi4scala.WrapOperation]] which as a reference to the current channel
   */
  abstract class WrapChannel extends WrapOperation {
    val ch = SimpleChannel.this
  }
  /**
   * Provides a full cycle pseudorandom sequence with a given modulus
   * @param length the modulus of the sequence
   * @return a clousure to a function which calculates the sequence
   */
  private def getGenerator(length: Int): (Int) => Int = {
    // the incrementer must be coprime with the modulus
    val inc = if (length % 17 == 0) 19 else 17
    (x: Int) => {
      (inc + x) % length
    }
  }
  /**
   * Add a [[pi4scala.Request]] to read from the current channel
   * @param r the [[pi4scala.Request]] to be added the channel
   */
  def addReadRequest(r: Request[A]) = {
    if (writeRequest.length == 0) {
      /*
       * if there are no write requests r cannot be immediatly served
       * so it is added to the array of request waiting for a write
       * request which serves it
       */
      readRequest += r
    } else if (!r.isComplete()) {
      /*
       * performs a single check if a request on an array can serve 
       * the given request
       */
      def performCheck(actual: Int, req: Request[A], array: ArrayBuffer[Request[A]]): Boolean = {
        val selected = array(actual)
        selected.getLock.synchronized {
          /*
           * the selected request in check should not be the same 
           * coming from req, if it was so it means there are already 
           * objects waiting for this channel. It means it is a 
           * nondeterministic write on the same channel. Also the
           * selected channel should not be already served because in that
           * case it would mean to different request of read are mapped to
           * the same request of write
           */
          if (!selected.getLock.eq(req.getLock) && !selected.isComplete()) {
            val (flag, res) = selected.getVal()
            req.setVal(res)
            array(actual) = array(array.length - 1)
            array.remove(array.length - 1)
            req.setComplete
            selected.setComplete
            true
          } else {
            false
          }
        }
      }
      /*
       * perform the check on each elements of writeRequest in order
       * to find if there is a writeRequest able to serve the readRequest.
       * Returns true iff there is such a request.
       */
      def checkExistence(actual: Int, gen: (Int) => Int, first: Int): Boolean = {
        if (actual == first)
          performCheck(first, r, writeRequest)
        else
          performCheck(actual, r, writeRequest) || checkExistence(gen(actual), gen, first)
      }
      // produce the full-cycle pseudorandom generator function
      val first = gen.nextInt(writeRequest.length)
      val f = getGenerator(writeRequest.length)
      if (!checkExistence(f(first), f, first)) {
        /*
         * if there are no writeRequest (the sequence is a full-cycle)
         * capable of serving the request it is added to the array of requests 
         */
        readRequest += r
      }
    }
  }

  /**
   * Add a [[pi4scala.Request]] to write to the current channel. Refear to
   * [[pi4scala.SimpleChannel.addReadRequest]] for further docs.
   * @param r the [[pi4scala.Request]] to be added the channel
   */
  def addWriteRequest(w: Request[A]) = {
    if (readRequest.length == 0) {
      writeRequest += w
    } else if (!w.isComplete()) {
      def performCheck(actual: Int, req: Request[A], array: ArrayBuffer[Request[A]]): Boolean = {
        val selected = array(actual)
        selected.getLock.synchronized {
          val (flag, res) = req.getVal()
          if (!selected.getLock.eq(req.getLock) && selected.setVal(res)) {
            req.setComplete
            selected.setComplete
            array(actual) = array(array.length - 1)
            array.remove(array.length - 1)
            true
          } else {
            false
          }
        }
      }
      def checkExistence(actual: Int, gen: (Int) => Int, first: Int): Boolean = {
        if (actual == first)
          performCheck(first, w, readRequest)
        else
          performCheck(actual, w, readRequest) || checkExistence(gen(actual), gen, first)
      }
      val first = gen.nextInt(readRequest.length)
      val f = getGenerator(readRequest.length)
      if (!checkExistence(f(first), f, first)) {
        writeRequest += w
      }
    }
  }
  /**
   * Ask to the current channel to remove any read or write request which
   *  references to a given request
   * @param r the request asked to remove from the channel
   */
  def removeRequest(r: Request[A]) = {
    readRequest -= r
    writeRequest -= r
  }
  /**
   * Returns the lock used to access to the current channel
   *  @return a [[scala.concurrent.Lock]] usable to perform an atomic access
   *  to the channel
   */
  def getLock(): ReentrantLock = lock

  /**
   * Performs a write from the given buffer to the current channel.
   * @param lb a [[pi4scala.LocalBuffer]] which provides the value to be
   * written
   * @return the value read from the buffer and input to the channel
   */
  private def write(lb: LocalBuffer[A]): A = {
    val sr = new SimpleRequest[A](lb)
    // acquire atomic access to the channel
    lock.acquire
    try {
      // actually add the request to write
      addWriteRequest(sr)
    } finally {
      lock.release
    }
    sr.synchronized {
      // wait until the request has been served
      while (!sr.isComplete())
        sr.wait()
    }
    lb.get
  }
  /**
   * Syntactic sugar for [[pi4scala.SimpleChannel.write]]
   */
  def <--(lb: LocalBuffer[A]): A = write(lb)
  /**
   * Produce a [[pi4scala.WrapOperation]] which contains a request
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
  def <==(lb: LocalBuffer[A])(exe: => Unit): WrapOperation =
    new WrapChannel {
      def update(sum: Choice) = {
        sum.addWrite(ch, lb, () => exe)
      }
    }
}
