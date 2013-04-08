package object pi4scala {
  private def defaultValue[U]: U = { class Default[U] {var default: U = _ }; new Default[U].default }
  def <--[A](chan: Channel[A]) : A = {
    new LocalBuffer[A](defaultValue[A]) <-- chan
  }
  def select(operations: WrapOperation*) = {
    val ch = new Choice
    for(arg <- operations)
      arg.update(ch)
    ch.execute
  }
  
  def scalaGo(func: () => Unit) = (new Thread(new Runnable{def run = func})).start
}