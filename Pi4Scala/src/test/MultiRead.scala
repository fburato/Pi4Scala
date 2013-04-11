package test
import pi4scala.{Channel, SimpleChannel, LocalBuffer, <--, select, scalaGo, Choice}
object MultiRead {

  def main(args: Array[String]): Unit = {
    val m1 = new SimpleChannel[Int]
    val m2 = new SimpleChannel[Int]
    def multiple(m1:Channel[Int],m2:Channel[Int]) = {
      val l = new LocalBuffer[Int](0)
      select(
        (l<==m1){
          println("Letto " + l + " da m1")
        },
        (l<==m2){
          println("Letto " + l + " da m2")
        }
      )
      select(
        (l<==m1){
          println("Letto " + l + " da m1")
        },
        (l<==m2){
          println("Letto " + l + " da m2")
        }
      )
    }
    def writer(chan: Channel[Int], v: Int) = {
      val l = new LocalBuffer[Int](v)
      chan <-- l
    }
    (new Thread(new Runnable{def run = multiple(m1,m2)})).start
    (new Thread(new Runnable{def run = {writer(m1,17)}})).start
    (new Thread(new Runnable{def run = {writer(m2,23)}})).start

  }

}