package test

import pi4scala._
import scala.util.Random.nextDouble
class Fork(n: String, t: Channel[AnyRef], l: Channel[AnyRef]) {
  val name = n
  val take = t
  val leave = l
  def run() {
    while (true) {
      <--(take)
      println("Fork " + n + " is taken")
      <--(leave)
    }
  }
}
object Philosophers {
  def philosopher(id: Int, left: Fork, right: Fork) =
    while (true) {
      println("Philosopher " + id + " is thinking...")
      Thread.sleep((nextDouble() * 2000).toLong)
      left.take <-- None
      Thread.sleep((nextDouble() * 2000).toLong)
      right.take <-- None
      println("Philosopher " + id + " is eating...")
      left.leave <-- None
      right.leave <-- None
    }
  def main(args: Array[String]): Unit = {
    val nPhil = 5;
    val forks = new Array[Fork](nPhil)
    for(i<- Range(0,nPhil)) {
      forks(i) = new Fork(""+i,new SimpleChannel[AnyRef],new SimpleChannel[AnyRef])
      scalaGo({forks(i).run})
    }
    for(i<- Range(0,nPhil-1)) {
      scalaGo({philosopher(i,forks(i),forks(i+1))})
    }
    // nodeadlock
    scalaGo({philosopher(nPhil-1,forks(0),forks(nPhil-1))})
    // deadlock
    //scalaGo({philosopher(nPhil-1,forks(nPhil-1),forks(0))})
  }

}
/*


// main
func main () {
  var forks [NPhil]Fork
  var i int
  
  // seeding
  rand.Seed(time.Now().UnixNano())

  for i=0; i<NPhil; i++ {

    // create the ith-fork
    forks[i] = Fork { 
      fmt.Sprintf("F%d",i),
      make(chan int), 
      make(chan int)}

    go forks[i].Run()
  }
  
  for i=0; i<NPhil-1; i++ {
    go Phil(i, &forks[i], &forks[(i+1)%NPhil])
  }
  
  // left-handed last philospher: no deadlock!
  // Phil(NPhil-1, &forks[0], &forks[NPhil-1])

  // right-handed last philospher: deadlock!
  Phil(NPhil-1, &forks[NPhil-1], &forks[0])
}

*/