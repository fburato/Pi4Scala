package test
import pi4scala.{Channel, SimpleChannel, LocalBuffer, <--, select, scalaGo, Choice}
object MultiRead {

  def main(args: Array[String]): Unit = {
    val m1 = new SimpleChannel[Int]
    val m2 = new SimpleChannel[Int]
    def multiple(m1:Channel[Int],m2:Channel[Int]) = {
      val l = new LocalBuffer[Int](0)
      val ch = new Choice();
      ch.addRead(l,m1, () => {
        println("Letto " + l + "da m1")
      })
      ch.addRead(l,m2, () => {
        println("Letto " + l + "da m2")
      })
      ch.execute()
      val ch1 = new Choice();
      ch1.addRead(l,m1, () => {
        println("Letto " + l + "da m1")
      })
      ch1.addRead(l,m2, () => {
        println("Letto " + l + "da m2")
      })
      ch1.execute()
      /*
      select(
        (l<==m1){
          println("Letto" + l + "da m1")
        },
        (l<==m2){
          println("Letto" + l + "da m2")
        }
      )
      select(
        (l<==m1){
          println("Letto" + l + "da m1")
        },
        (l<==m2){
          println("Letto" + l + "da m2")
        }
      )*/
    }
    def writer(chan: Channel[Int], v: Int) = {
      val l = new LocalBuffer[Int](v)
      chan <-- l
    }
    (new Thread(new Runnable{def run = multiple(m1,m2)})).start
    (new Thread(new Runnable{def run = {writer(m1,17)}})).start
    (new Thread(new Runnable{def run = {writer(m2,23)}})).start

  }
  /*
   *
    Runnable writer1 = new Runnable() {
      public void run() {
        LocalBuffer<Integer> lb = new LocalBuffer<Integer>(17);
        m1.write(lb);
      }
    };
    Runnable writer2 = new Runnable() {
      public void run() {
        LocalBuffer<Integer> lb = new LocalBuffer<Integer>(23);
        m2.write(lb);
      }
    };
    
    (new Thread(multiple)).start();
    (new Thread(writer1)).start();
    (new Thread(writer2)).start();
   */
}