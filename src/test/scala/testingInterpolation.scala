import org.apache.commons.math3.analysis.interpolation.SplineInterpolator
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction


@main
def testInterpolation(): Unit =
  val rangesInclusive = (start: Int, end: Int, steps: Int) => (0 to steps).map(start + _ * ((end - start) / steps.toDouble))


  val y = Array[Double](0, 20, 10, 30, 49, 50, 5)
  val x = Array[Double](0, 20, 40, 60, 80, 100, 120)

  val y1 = Array[Double](0, 100, 5, 120, 0)
  val x1 = Array[Double](0, 10, 20, 30, 40)
  val testy = y1.sliding(3).toArray
  val test = x1.sliding(3).toArray


  // here we check if we would have the points (30, 120) -> (10, 20),
  // can we model this correctly if we put them in ascending order
  val y2 = Array[Double](0, 20, 120)
  val x2 = Array[Double](0, 10, 30)

  val interPolateFunction = SplineInterpolator().interpolate(x, y)
  val interPolateFunction1 = SplineInterpolator().interpolate(x1, y1)
  val interPolateFunction2 = SplineInterpolator().interpolate(x2, y2)

  val a = rangesInclusive(0, 100, 50)
  val b = rangesInclusive(0, 30, 50)
  println(interPolateFunction1.getKnots.mkString("Array(", ", ", ")"))
  //for value <- b do
  //  println(interPolateFunction2.value(value))

  for i <- test.indices do
    val ay  = testy(i)
    val ax = test(i)
    val interpolator = SplineInterpolator().interpolate(ax, ay)

    val ranges = rangesInclusive(ax.head.toInt, ax.last.toInt, 50)
    for value <- ranges do
      println(interpolator.value(value))


  println(rangesInclusive(0, 100, 3))