package pi4scala

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object SimpleChannel {
  private def defaultValue[U]: U = { class Default[U] {var default: U = _ }; new Default[U].default }
  implicit def <--[A](chan: Channel[A]) : A = {new LocalBuffer[A](defaultValue[A]) <-- chan}
}

class SimpleChannel[A] extends Channel[A] {
  val readRequest = new ArrayBuffer[Request[A]]
  val writeRequest = new ArrayBuffer[Request[A]]
  val gen = new  Random()
  abstract class WrapChannel extends WrapOperation {
    val ch = SimpleChannel.this
  }
  def addReadRequest(r: Request[A]) : Boolean = synchronized{
    if(writeRequest.length == 0) {
      readRequest += r
      true
    } else if(! r.isComplete()){
      val random = gen.nextInt(writeRequest.length)
      val selected = writeRequest(random)
      val (flag,res) = selected.getVal()
      writeRequest(random) = writeRequest(writeRequest.length-1)
      writeRequest.remove(writeRequest.length -1)
      if(flag && r.setVal(res)) {
        r.setComplete
        selected.setComplete
        true
      } else {
        false
      }
    } else {
      true
    }
  }
  def addWriteRequest(w: Request[A]) :Boolean = synchronized {
    if(readRequest.length == 0) {
      writeRequest += w
      true
    } else if(! w.isComplete()){
      val random = gen.nextInt(readRequest.length)
      val selected = readRequest(random)
      val (flag,res) = w.getVal()
      readRequest(random) = readRequest(readRequest.length-1)
      readRequest.remove(readRequest.length -1)
      if(flag && selected.setVal(res)) {
        w.setComplete
        selected.setComplete
        true
      } else {
        false
      }
    } else {
      true
    }
  }
  def removeRequest(r: Request[A]) = synchronized{
    readRequest -= r
    writeRequest -= r
  }
  
  private def write (lb: LocalBuffer[A]) : A = {
    val sr = new SimpleRequest[A](lb)
    while(!addWriteRequest(sr)){}
    sr.synchronized {
      while(!sr.isComplete()) sr.wait()
    }
    lb.get
  }
  def <--(lb: LocalBuffer[A]) : A = write(lb)
  def <==(lb: LocalBuffer[A])(exe: => Unit): WrapOperation = 
    new WrapChannel {
    def update(sum: Choice) = {
      sum.addWrite(ch,lb,() => exe)
    }
  }
    //new WriteOperation(this,lb,() => exe)
}
