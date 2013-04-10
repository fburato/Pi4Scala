package test

import pi4scala._
object SameObject {

  def main(args: Array[String]): Unit = {
    val ch = new SimpleChannel[AnyRef]
    val l = new LocalBuffer[AnyRef](None)
    scalaGo({select(
      (ch <== l){
        println("Scritto sul canale")
      },
      (l <== ch) {
        println("Letto dal canale")
      }
    )})
  }

}