package test

import pi4scala._

object RandomSelect {
  def main(argv: Array[String]):Unit = {
    val ch = new SimpleChannel[Int]
    val l = new LocalBuffer[Int](0)
    scalaGo({
      select(
        (ch <== 17) {
          println("Scritto sul canale 17")
        },
        (ch <== 18) {
          println("Scritto sul canale 18")
        })
      val l = new LocalBuffer[Int](0)
      l <-- ch
      println("Letto "+ l)
    })
    l <-- ch
    println("Letto " + l)
    ch <-- 41
  }
}