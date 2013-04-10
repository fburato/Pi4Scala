package pi4scala

import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.concurrent.Lock

object SimpleChannel {
  private def defaultValue[U]: U = { class Default[U] { var default: U = _ }; new Default[U].default }
  implicit def <--[A](chan: Channel[A]): A = { new LocalBuffer[A](defaultValue[A]) <-- chan }
}

class SimpleChannel[A] extends Channel[A] {
  private val readRequest = new ArrayBuffer[Request[A]]
  private val writeRequest = new ArrayBuffer[Request[A]]
  private val gen = new Random()
  val lock = new Lock

  abstract class WrapChannel extends WrapOperation {
    val ch = SimpleChannel.this
  }

  def addReadRequest(r: Request[A]) = {
    if (writeRequest.length == 0) {
      readRequest += r
    } else if (!r.isComplete()) {
      def performCheck(actual: Int, req: Request[A], array: ArrayBuffer[Request[A]]): Boolean = {
        val selected = array(actual)
        selected.getLock.synchronized {
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
      def checkExistence(actual: Int, gen: (Int) => Int, first: Int): Boolean = {
        if (actual == first)
          performCheck(first, r, writeRequest)
        else
          performCheck(actual, r, writeRequest) || checkExistence(gen(actual), gen, first)
      }
      val first = gen.nextInt(writeRequest.length)
      val f = getGenerator(writeRequest.length)
      if (!checkExistence(f(first), f, first)) {
        readRequest += r
      }
    }
  }
  private def getGenerator(length: Int): (Int) => Int = {
    val inc = if (length % 17 == 0) 19 else 17
    (x: Int) => {
      (inc + x) % length
    }
  }

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

  def removeRequest(r: Request[A]) = {
    readRequest -= r
    writeRequest -= r
  }

  def getLock(): Lock = lock

  private def write(lb: LocalBuffer[A]): A = {
    val sr = new SimpleRequest[A](lb)
    lock.acquire
    try {
      addWriteRequest(sr)
    } finally {
      lock.release
    }
    sr.synchronized {
      while (!sr.isComplete())
        sr.wait()
    }
    lb.get
  }
  
  def <--(lb: LocalBuffer[A]): A = write(lb)
  
  def <==(lb: LocalBuffer[A])(exe: => Unit): WrapOperation =
    new WrapChannel {
      def update(sum: Choice) = {
        sum.addWrite(ch, lb, () => exe)
      }
    }
}
