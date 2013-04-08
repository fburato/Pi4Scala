package test

import pi4scala.{SimpleChannel, LocalBuffer, <--}

object BasicCom {

  def main(args: Array[String]): Unit = {
    val chan = new SimpleChannel[Int]
    val sync = new SimpleChannel[Any]
    val starter = new SimpleChannel[Any]
    val total = 100
    val pong = () => {
      var cont = true
      val l = new LocalBuffer[Int](0)
      val s = new LocalBuffer[Any]
      while (cont) {
        l <-- chan
        s <-- sync
        if (l <= total)
          println("Pong " + l)
        else {
          cont = false
          println("Fine Pong")
        }
        chan <-- l
      }
      starter <-- None
    }
    val ping = () => {
      var cont = true
      val l = new LocalBuffer[Int](0)
      val s = new LocalBuffer[Any]
      s <-- starter
      while (cont) {
        chan <-- l
        if (l <= total)
          println("Ping " + l)
        else {
          cont = false
          println("Fine Ping")
        }
        sync <-- s
        l <-- chan
        l := 1 + l
      }
      
    }
    val t1 = new Thread(new Runnable { def run() { ping() } })
    val t2 = new Thread(new Runnable { def run() { pong() } })
    t1.start
    t2.start
    starter <-- None
    val l = <--(starter)
  }
  
}