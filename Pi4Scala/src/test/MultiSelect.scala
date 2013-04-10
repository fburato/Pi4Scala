package test

import pi4scala._

object MultiSelect {

  def main(args: Array[String]): Unit = {
    val ch1 = new SimpleChannel[AnyRef]
    val ch2 = new SimpleChannel[AnyRef]
    val ch3 = new SimpleChannel[AnyRef]
    val ch4 = new SimpleChannel[AnyRef]
    def s(name: String, chan1: Channel[AnyRef], chan2: Channel[AnyRef]) = {
      val l = new LocalBuffer[AnyRef](None)
      select(
          (l<==chan1) {
            println(name + " ha letto")
          },
          (chan2<==l) {
            println(name + " ha scritto")
          }
      )
    }
    scalaGo({s("proc1",ch1,ch2)})
    scalaGo({s("proc2",ch2,ch1)})
    scalaGo({s("proc3",ch1,ch2)})
    scalaGo({s("proc4",ch2,ch1)})
  }

}