package pi4scala

import scala.util.Random

class Choice {
  private class ChoiceRequest[A](lb: LocalBuffer[A], e: () => Unit) extends Request[A] {
    def setVal(v: A): Boolean = Choice.this.synchronized {
      if (Choice.this.isComplete())
        false
      else {
        lb.set(v)
        Choice.this.setNext(e)
        true
      }
    }
    def getVal(): (Boolean, A) = Choice.this.synchronized {
      if (Choice.this.isComplete())
        (false, lb get)
      else {
        Choice.this.setNext(e);
        (true, lb get)
      }
    }
    def isComplete(): Boolean = Choice.this.synchronized {
      Choice.this.isComplete()
    }
    def setComplete() = Choice.this.synchronized {
      Choice.this.notify
    }
  }
  private var e: () => Unit = () => {}
  private var complete: Boolean = false
  var requests: List[() => Unit] = Nil
  var removers: List[() => Unit] = Nil
  def setNext(newe: () => Unit) {
    e = newe
    complete = true
  }

  def isComplete(): Boolean =
    complete

  private def exec(l: List[() => Unit]): Unit = l match {
    case List() =>
    case el :: rem => {
      el()
      exec(rem)

    }
  }

  def execute() = {
    val shuffled = Random.shuffle(requests)
    exec(shuffled)
    synchronized {
      while (!isComplete()) wait()
    }
    exec(removers)
    e()
  }
  
  def addRead[A](lb: LocalBuffer[A], chan: Channel[A], executor: () => Unit) = {
    val r = new ChoiceRequest(lb, executor)
    requests = (() => {
      while (!chan.addReadRequest(r)) {}
    }) :: requests
    removers = (() => {
      chan.removeRequest(r)
    }) :: removers
  }

  def addWrite[A](chan: Channel[A], lb: LocalBuffer[A], executor: () => Unit) = {
    val r = new ChoiceRequest(lb, executor)
    requests = (() => {
      while (!chan.addWriteRequest(r)) {}
    }) :: requests
    removers = (() => {
      chan.removeRequest(r)
    }) :: removers
  }

}