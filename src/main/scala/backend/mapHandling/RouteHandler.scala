package backend.mapHandling

import backend.Range
import backend.jsonModels.MapDeserialized
import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator

import scala.collection.mutable.ArrayBuffer
import scala.math.abs

/**
 * Luokka sisältää kaikki eri osiot vihollisten etenemisreitistä ja tekee mahdolliseksi
 * myös vihollisten taaksepäin etenemisen.
 *
 * Soveltaa useita UnivariateFunction olioita Apache-kirjastosta
 * mahdollistamaan etenemisen mihin tahansa suuntaan
 * @param routeWidth reitin leveys
 * @param map MapDeserialized-olio, joka sisältää kartan id ja sen reitin pisteet.
 */
class RouteHandler(val routeWidth: Int, private val map: MapDeserialized):

  val startingPoint = map.roadPoints.head

  /*
   * rakennetaan kaikki reitit
  */
  private val auxRoutes = ArrayBuffer[PolynomialSplineFunction]()
  private val auxOppositeDirection = ArrayBuffer[Boolean]()
  private var goingOpposite = false
  private var justChanged = false

  private var lastPointX = map.roadPoints.head.x
  private val pointIterator = map.roadPoints.iterator

  private val currPoints = ArrayBuffer[Coordinates](map.roadPoints.head)
  private var nextPoint = pointIterator.next() // otetaan ensimmäinen pois iteraatioista, koska se on jo lastPointX:ssä
  while pointIterator.hasNext do
    nextPoint = pointIterator.next()

    // jos x on pienempi ja aiemmin arvot eivät laskeneet, niin suunta vaihtui
    if nextPoint.x <= lastPointX && !goingOpposite then
      val x = currPoints.map(_._1).toArray
      val y = currPoints.map(_._2).toArray

      // varmista, että on vähintään 3 pistettä
      if x.length < 3 then throw Exception("You need at least 3 routepoints to create a part of route, the given points may be malformed")

      val interPolateFunction = SplineInterpolator().interpolate(x, y)

      auxRoutes += interPolateFunction
      auxOppositeDirection += false

      goingOpposite = true
      currPoints.clear()


    else if nextPoint.x >= lastPointX && goingOpposite then
      val x = currPoints.map(_._1).toArray.reverse
      val y = currPoints.map(_._2).toArray.reverse

      // varmista, että on vähintään 3 pistettä
      if x.length < 3 then throw Exception("You need at least 3 routepoints to create a part of route, the given points may be malformed")

      val interPolateFunction = SplineInterpolator().interpolate(x, y)

      auxRoutes += interPolateFunction
      auxOppositeDirection += true

      goingOpposite = false
      currPoints.clear()

    currPoints += nextPoint
    lastPointX = nextPoint.x


  if !goingOpposite then
    val x = currPoints.map(_._1).toArray
    val y = currPoints.map(_._2).toArray

    // varmista, että on vähintään 3 pistettä
    if x.length < 3 then throw Exception("You need at least 3 routepoints to create a part of route, the given points may be malformed")

    val interPolateFunction = SplineInterpolator().interpolate(x, y)

    auxRoutes += interPolateFunction
    auxOppositeDirection += false

    goingOpposite = false
    currPoints.clear()


  else if goingOpposite then
    val x = currPoints.map(_._1).toArray.reverse
    val y = currPoints.map(_._2).toArray.reverse

    // varmista, että on vähintään 3 pistettä
    if x.length < 3 then throw Exception("You need at least 3 routepoints to create a part of route, the given points may be malformed")

    val interPolateFunction = SplineInterpolator().interpolate(x, y)

    auxRoutes += interPolateFunction
    auxOppositeDirection += true

    goingOpposite = true
    currPoints.clear()

  // -------------------------


  val routes: Array[PolynomialSplineFunction] = auxRoutes.toArray
  private var routeIdx = 0
  private val derivatives: Array[UnivariateFunction] = routes.map(_.derivative())
  private val oppositeDirection: Array[Boolean] = auxOppositeDirection.toArray
  private var currDerivatives = derivatives.head
  private var currOperator = 1 // vaihtelee 1 ja -1 arvojen välillä, jotta saadaan + tai -


  /**
   * Antaa seuraavan pisteen, johon siirryttäisiin, jos kuljettaisiin
   * annetusta sijainnista annetulla nopeudella.
   * @param currX tämänhetkinen sijainti
   * @param speed nopeus, jolla kuljetaan,
   * @return seuraava piste, johon saavuttaisiin annetulla nopeudella
   */
  def nextLocation(currX: Double, speed: Double): Option[Coordinates] =
    if routeIdx >= routes.length then return None
    // viimeinen arvo tämänhetkisessä reitissä
    var lastValue = 0.0

    val denominator = math.sqrt(math.pow(currDerivatives.value(currX), 2) + 1) // nimittäjä
    val dx = speed / denominator

    // jos ylitettiin tämänhetkinen reitti, niin
    // vaihda seuraavaan reittiin
    if currOperator == 1 then
      lastValue = routes(routeIdx).getKnots.last

      if currX + dx > lastValue then
        routeIdx += 1

    else
      lastValue = routes(routeIdx).getKnots.head
      if currX + (dx * currOperator) < lastValue then
        routeIdx += 1


    if routeIdx >= routes.length then
      return None

    if oppositeDirection(routeIdx) then
      currOperator = -1

    else
      currOperator = 1

    currDerivatives = derivatives(routeIdx)

    val denominator1 = math.sqrt(math.pow(currDerivatives.value(currX), 2) + 1) // nimittäjä
    val dx1 = speed / denominator1

    val nextValue = routes(routeIdx).value(currX + ( dx1 * currOperator ))

    Some( Coordinates(currX + ( dx1 * currOperator ), nextValue) )


  def canPlace(towerProperties: Range): Boolean =
    var idx = 0
    val n = routes.length
    val location = towerProperties.location

    var canPlace: Boolean = true

    while idx < n do
      val aDerivative = derivatives(idx)
      val aRoute = routes(idx)

      if aRoute.getKnots.head < location.x && location.x < aRoute.getKnots.last then
          val k = aDerivative.value(towerProperties.location.x) // kulmakerroin
          val c = routes(idx).value(towerProperties.location.x) - k * towerProperties.location.x // kiintopisteen arvo

          val numerator: Double = abs(-k * towerProperties.location.x - c + towerProperties.location.y)
          val denominator = math.sqrt(k * k + 1 * 1) // kulmakerroin kerrottuna itsellään antaa aina positiivisen arvon tai nollan

          val res = numerator / denominator

          if res < towerProperties.radius + routeWidth.toDouble / 2 then
            canPlace = false

            return canPlace

      idx += 1

    canPlace


  def id = map.id

  def copy: RouteHandler = RouteHandler(routeWidth, map)

  def copyFull: RouteHandler =
    val newRouteHandler = RouteHandler(routeWidth, map)
    newRouteHandler.routeIdx = scala.math.min(routeIdx, routes.length - 1)
    newRouteHandler.currDerivatives = newRouteHandler.derivatives(newRouteHandler.routeIdx)
    newRouteHandler.currOperator = currOperator

    newRouteHandler

end RouteHandler