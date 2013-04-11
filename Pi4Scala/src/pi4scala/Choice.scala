package pi4scala

import scala.util.Random
import scala.concurrent.Lock

/**
 * Provides a unique lock in order to prevent multiple
 * Pi-calculus summation to be resolved by multiple threads.
 */
private[pi4scala] object Choice {
  val choiceLock = new Lock
}

/**
 * An object representing a summation in Pi-calculus.
 *
 * It accepts multiple read and writes requests from different channel and
 * when executed serves (nondeterministically) only one of them which is
 * executable i.e. that as received the waited write or read request from
 * another thread.
 */
private[pi4scala] class Choice {

  /**
   * A Request which use the outer Choice as synchronizable object
   * and as reference to communicate the completeness of the Request itself.
   *
   * @constructor creates a new ChoiceRequest with a given buffer and closure
   * @param lb a [[pi4scala.LocalBuffer]] usable for performing read and write operations
   * @param e the closure to the function to be executed when the current
   * Request is completed
   */
  private class ChoiceRequest[A](lb: LocalBuffer[A], e: () => Unit) extends Request[A] {
    def setVal(v: A): Boolean = {
      lb.set(v)
      true
    }
    def getVal(): (Boolean, A) = {
      (true, lb get)
    }
    def isComplete(): Boolean = {
      Choice.this.isComplete()
    }

    /**
     * Sets the closure that as to be executed from the summation
     * to the constructing closure of this Request.
     *
     * This is due to the
     * fact that when the request is set as complete just its associated
     * closure must be executed.
     */
    def setComplete() = Choice.this.synchronized {
      Choice.this.setNext(e)
      Choice.this.notify
    }
    /**
     * Returns, as object to which synchronize, the outer Choice
     */
    def getLock() = Choice.this
  }
  private var e: () => Unit = () => {}
  private var complete: Boolean = false
  private var requests: List[() => Unit] = Nil
  private var removers: List[() => Unit] = Nil
  private var locks: List[Lock] = Nil
  
  /**
   * Sets the closure to be executed when one of the Choice requests
   * has been satisfate.
   * 
   * @param neww a closure to the function to execute
   */
  private def setNext(newe: () => Unit) {
    e = newe
    complete = true
  }

  /**
   * Returns if the current choice has already been completed or not
   * 
   * @return true iff the current Choice has been completed
   */
  def isComplete(): Boolean =
    complete

  /**
   * Given a List of closure execute every of them.
   * 
   * @param l a list of closure to be executed
   */
  private def exec(l: List[() => Unit]): Unit = l match {
    case List() =>
    case el :: rem => {
      el()
      exec(rem)

    }
  }

  /**
   * Execute the summation at the state of its requests.
   * 
   * After the read and write requests are added to the summation,
   * the execution of execute() determines the wait of the thread
   * until one of its request are completed. Just one execute() of
   * different Choice should be executed at a given time since
   * it is not possible to guarantee integrity of  them
   * if multiple request are added at the same time.
   */
  def execute() = {
    // acquire common lock of choices
    Choice.choiceLock.acquire
    try {
      /*
       * Acquire the lock of every channel in the requests since
       * no thread should interrupt the current while adding requests.
       * 
       * Note that since every request performed to a channel are unique
       * (i.e. there are not multiple channel synchronized in a channel
       * and the same request) there is no deadlock.
       */
      def lockAll(list: List[Lock]): Unit = list match {
        case Nil => {}
        case head :: tail => {
          head.synchronized {
            if (head.available)
              // control to prevent auto deadlock since Lock are not reentrant
              head.acquire
          }
          lockAll(tail)
        }
      }
      /*
       * Release the lock of every channel in the requests
       * when finishing the adding process 
       */
      def unlockAll(list: List[Lock]): Unit = list match {
        case Nil => {}
        case head :: tail => {
          head.release
          unlockAll(tail)
        }
      }
      // lock all channels
      lockAll(locks)
      try {
        //randomize requests to guarantee nondeterminism
        val shuffled = Random.shuffle(requests)
        // ex
        exec(shuffled)
      } finally {
        // unlocks all channels
        unlockAll(locks)
      }
    } finally {
      Choice.choiceLock.release
    }
    // when completed the add*Request to every channel wait until summation completes
    synchronized {
      while (!isComplete()) wait()
    }
    // removes every remaining requests from the channels
    exec(removers)
    //execute just the selected rest of the code
    e()
  }
  /**
   * Add a read request to the summation.
   * 
   * @param lb a [[pi4scala.LocalBuffer]] which will contain the information 
   * @param chan a [[pi4scala.Channel]] from which reading information
   * @param executor the closure of the following block to be executed 
   */
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
  /**
   * Add a write request to the summation.
   * 
   * @param lb a [[pi4scala.LocalBuffer]] from which reading information
   * @param chan a [[pi4scala.Channel]] from to which writing information
   * @param executor the closure of the following block to be executed 
   */
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