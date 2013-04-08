package pi4scala

abstract class Request[A] {
  def setVal(v :A) :Boolean
  def getVal() : (Boolean,A)
  def isComplete() : Boolean
  def setComplete()
}

/*
 * public interface Request<A> {
  public boolean setVal(A val);
  public Couple<A> getVal();
  public boolean isComplete();
  public void setComplete();
}
 * 
 */