package pi4scala

/**
 * Given a [[pi4scala.Choice]] perform a given operation on it 
 */
private[pi4scala] abstract class WrapOperation {
  def update(sum: Choice)
}