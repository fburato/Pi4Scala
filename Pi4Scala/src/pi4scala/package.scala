/**
 * Package object for pi4scala. Contains some methods useful to
 * have syntactic sugar.
 */
package object pi4scala {
  private def defaultValue[U]: U = { class Default[U] { var default: U = _ }; new Default[U].default }
  /**
   * Syntactic sugar for reading from channel without any [[pi4scala.LocalBuffer]]
   * @param chan a [[pi4scala.Channel]] from which read information
   */
  def <--[A](chan: Channel[A]): A = {
    new LocalBuffer[A](defaultValue[A]) <-- chan
  }
  /**
   * Syntactic sugar for executing in just one method call a summation
   * in which use operators "<==" and "==>"
   * @param operations list of [[pi4scala.WrapOperation]] to add to a new summation
   */
  def select(operations: WrapOperation*) = {
    val ch = new Choice
    for (arg <- operations)
      arg.update(ch)
    ch.execute
  }
  /**
   * Executor of an anonymous function as a new Thread
   * @param func closure passed by value which will be the run method of the
   * new thread
   */
  def scalaGo(func: => Unit) = (new Thread(new Runnable { def run = func })).start
}