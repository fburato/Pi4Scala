package pi4scala

import scala.util.Random
import scala.concurrent.Lock
object Choice {
  val choiceLock = new Lock
}

class Choice {
  private class ChoiceRequest[A](lb: LocalBuffer[A], e: () => Unit) extends Request[A] {
    def setVal(v: A): Boolean =  {
      lb.set(v)
      true
    }
    def getVal(): (Boolean, A) =  {
        (true, lb get)
    }
    def isComplete(): Boolean =  {
      Choice.this.isComplete()
    }
    def setComplete() = Choice.this.synchronized {
      Choice.this.setNext(e)
      Choice.this.notify
    }
    def getLock() = Choice.this
  }
  private var e: () => Unit = () => {}
  private var complete: Boolean = false
  private var requests: List[() => Unit] = Nil
  private var removers: List[() => Unit] = Nil
  private var locks: List[Lock] = Nil
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
    Choice.choiceLock.acquire
    try {
      def lockAll(list: List[Lock]): Unit = list match  {
        case Nil => {}
        case head::tail => {
          head.synchronized{
            if(head.available)
              head.acquire
          }
          lockAll(tail)
        }
      }
      def unlockAll(list: List[Lock]): Unit = list match {
        case Nil => {}
        case head::tail => {
          head.release
          unlockAll(tail)
        }
      } 
      lockAll(locks)
      try {
        val shuffled = Random.shuffle(requests)
        exec(shuffled)
      } finally {
        unlockAll(locks)
      }
    } finally {
      Choice.choiceLock.release
    }
    synchronized {
      while (!isComplete()) wait()
    }
    exec(removers)
    e()
  }
  
  def addRead[A](lb: LocalBuffer[A], chan: Channel[A], executor: () => Unit) = {
    val r = new ChoiceRequest(lb, executor)
    requests = (() => {
      chan.addReadRequest(r)
    }) :: requests
    removers = (() => {
      val l = chan.getLock
      l.acquire
      try {
        chan.removeRequest(r)
      } finally {
        l.release
      }
    }) :: removers
    locks = chan.getLock :: locks
  }

  def addWrite[A](chan: Channel[A], lb: LocalBuffer[A], executor: () => Unit) = {
    val r = new ChoiceRequest(lb, executor)
    requests = (() => {
      chan.addWriteRequest(r)
    }) :: requests
    removers = (() => {
      chan.removeRequest(r)
    }) :: removers
    locks = chan.getLock :: locks
  }

}