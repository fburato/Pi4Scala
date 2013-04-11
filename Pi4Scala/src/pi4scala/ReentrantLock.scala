/**
 * File: ReentrantLock.scala
 * Package: pi4scala
 * Autore: Francesco Burato
 * Creazione: 11/apr/2013
 */
package pi4scala

/**
 * A Reentrant lock used to be locked multiple time by the same thread
 * @author Francesco Burato
 *
 */
class ReentrantLock {
  private var available = 0
  private var holdingThread : Thread = null
  /**
   * Tries to acquire the Lock.
   * 
   * If there is some other thread holding it, it causes the currentThread 
   * to wait until notify. If holding thread is executing again the method it just
   * increment the available counter.
   */
  def acquire() = synchronized {
    while (available > 0 && holdingThread != Thread.currentThread()) 
      wait()
    available += 1
    holdingThread = Thread.currentThread()
  }
  /**
   * Releases the Lock.
   * 
   * It should be called just by the holdingThread and decrements the
   * available counter until it reaches 0. When it happens waiting thread
   * are notified.
   */
  def release() = synchronized {
    if(available > 0) {
      available -= 1
      if(available == 0){
        notify
        holdingThread = null
      }
    }
  }
  
  def getAvailable() = synchronized {
    available
  }
}