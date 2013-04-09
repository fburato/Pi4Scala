package pi4scala
object LocalBuffer {
  implicit def toLocalBuffer[A](v: A) = new LocalBuffer[A](v)
  implicit def toValueA[A](lb: LocalBuffer[A]): A = lb get
}
class LocalBuffer[A](init: A) {
  abstract class WrapBuffer extends WrapOperation {
    val lb = LocalBuffer.this
  }
  var value = init
  def get = value
  def set(a: A) = { value = a }
  def read(chan: Channel[A]): A = {
    val sr = new SimpleRequest[A](this)
    chan.getLock.acquire
    try {
      while (!chan.addReadRequest(sr)) {}
    } finally {
      chan.getLock.release
    }
    sr.synchronized {
      while (!sr.isComplete) {
        sr.wait()
      }
    }
    value
  }
  def <--(chan: Channel[A]) = read(chan)

  def <==(chan: Channel[A])(exe: => Unit): WrapOperation =
    new WrapBuffer {
      def update(sum: Choice) = {
        sum.addRead(lb, chan, () => exe)
      }
    }
  //new ReadOperation[A](chan,this,() => exe)

  def :=(v: A) = set(v)

  override def toString(): String = value.toString()
}
