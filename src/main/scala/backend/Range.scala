package backend

import backend.mapHandling.Coordinates

case class Range(radius: Int, location: Coordinates):

  // optimoi inside() komennon laskemisen vähentämällä
  // ylimääräisen potenssilaskun vain kerran laskettavaksi
  val radiusSquared = radius * radius

  /**
   * Määrittää, että onko annettu sijainti ympyrän sisällä
   * @param otherLoc  sijainti, joka tarkistetaan
   * @return
   */
  def inside(otherLoc: Coordinates): Boolean =
    ( (location.x - otherLoc.x) * (location.x - otherLoc.x) ) + ( (location.y - otherLoc.y) * (location.y - otherLoc.y) ) <= radiusSquared

  def * (multiplier: Double): Range = Range((radius * multiplier).toInt, location)
  def + (other: Double): Range = Range((radius + other).toInt, location)

  def * (multiplier: Range): Range = Range(radius * multiplier.radius, location)
  def + (other: Range): Range = Range(radius + other.radius, location)

end Range