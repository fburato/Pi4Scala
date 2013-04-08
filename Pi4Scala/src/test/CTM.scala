package test
import pi4scala.{Channel, SimpleChannel, LocalBuffer, <--, select}
object CTM {

  def main(args: Array[String]): Unit = {
    val coin = new SimpleChannel[Any]
    val tea = new SimpleChannel[Any]
    val coffee = new SimpleChannel[Any]
    def ctm() = {
      var total = 100
      val l = new LocalBuffer[Any](None)
      while(total > 0) {
        l <-- coin
        println("La macchina ha accettato un gettone")
        select(
            (coffee <== l){
                println("Servito un caffè")
            },
            (tea <== l){
              println("Servito un tea")
            }
        )
        total = total - 1
      }
      println("Fine elaborazione")
    }
    def cs(s: String, chan: Channel[Any]) {
      val l = new LocalBuffer[Any](None)
      while(true){
        println("Il CS del " + s + " ha pubblicato una rivista")
        coin <-- l
        println("Il CS del " + s + " ha inserito un gettone")
        l <-- chan
        println("Il CS del " + s + " ha inserito bevuto")
      }
    }
    (new Thread(new Runnable{def run = ctm})).start
    (new Thread(new Runnable{def run = cs("caffè",coffee)})).start
    (new Thread(new Runnable{def run = cs("tea",tea)})).start
  }

}
/*
 * 
   
 */