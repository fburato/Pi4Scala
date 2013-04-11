/**
 * File: WrapOperation.scala
 * Package: pi4scala
 * Autore: Francesco Burato
 * Creazione: 11/apr/2013
 */
package pi4scala

/**
 * Given a [[pi4scala.Choice]] perform a given operation on it 
 * 
 * @author Francesco Burato
 */
private[pi4scala] abstract class WrapOperation {
  def update(sum: Choice)
}