package backend.mapHandling

import upickle.default.ReadWriter

/**
 * Koordinaattiluokka helpottamaan toimintoja
 * @param x x-koordinaatti
 * @param y y-koordinaatti
 */
case class Coordinates(var x: Double, var y: Double) derives ReadWriter:

  def inRange(x1: Double, y1: Double): Boolean = x >= x1 && y1 <= y

  def length: Double = math.sqrt( x * x + y * y )

  inline def +=(coord: Coordinates): Unit =
    x += coord.x
    y += coord.y

  inline def -(coord: Coordinates): Coordinates = Coordinates(x - coord.x, y - coord.y)
  inline def * (multiplier: Double): Coordinates = Coordinates(x * multiplier, y * multiplier)

end Coordinates