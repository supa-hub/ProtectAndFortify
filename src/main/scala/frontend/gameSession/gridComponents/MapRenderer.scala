package frontend.gameSession.gridComponents

import backend.mapHandling.Coordinates
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction
import scalafx.collections.ObservableBuffer
import scalafx.scene.canvas.Canvas
import scalafx.scene.image.Image
import scalafx.scene.layout.Pane
import scalafx.scene.paint.Color.{Blue, Gray, Orange}
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.Font
import scalafx.scene.Node

import scala.collection.mutable.ListBuffer
import scala.math.{atan, toDegrees}

/**
 * Luokka, joka käsittelee kartan renderöimisen pelin aikana
 * @param mapWidth kartan leveys
 * @param mapHeight kartan korkeus
 * @param mapURI osoitelähde, joka sisältää kartalle piirrettävien asioiden kuvat
 * @param imageName kartan taustan kuvan nimi
 * @param onClickAction tapahtuma, joka tapahtuu, kun karttaa painetaan
 */
class MapRenderer(
  private val mapWidth: Int,
  private val mapHeight: Int,
  private val mapURI: String,
  private val imageName: String,
  onClickAction: Coordinates => Unit = (_: Coordinates) => ()
  ):

  private var privateRenderablesCount = 0

  // piirtää backroundin
  private val canvas = new Canvas(mapWidth, mapHeight):
    mouseTransparent = true

  canvas.clip = new Rectangle:
    width = mapWidth
    height = mapHeight


  // Getting the GraphicsContext
  private val g = canvas.graphicsContext2D

  //Simple drawing.
  g.fill = Gray // Set the fill color.
  g.fillRect(0, 0, mapWidth, 450) // Fill rectangle at (0, 0) with mapWidth 600 and mapHeight 450.
  g.fill = Blue
  g.font = Font(50) // Set text size


  g.drawImage(Image(s"$mapURI/${imageName}.jpg"), 0, 0, mapWidth, mapHeight)


  // sisältää kaikki piirrettävät elementit.
  private val map = new Pane():
    pickOnBounds = true
    onMouseClicked = (me) =>
      permanentRenderables.clear()
      me.getPickResult.getIntersectedNode match
        // jos painetaan jotain kuvaa, niin täytyy kutsua sen
        // tapahtumia, muulloin käsitellään koko kartan MouseEvent
        case iv: javafx.scene.image.ImageView => iv.fireEvent(me.copyFor(iv, iv))
        case _ =>
          val clickLocation = Coordinates(me.getX, me.getY)
          onClickAction(clickLocation)
          me.consume()


  map.clip = new Rectangle:
    width = mapWidth
    height = mapHeight

  map.maxWidth = mapWidth
  map.maxHeight = mapHeight
  map.minWidth = mapWidth
  map.minHeight = mapHeight
  map.prefWidth = mapWidth
  map.prefHeight = mapHeight


  val renderables = map.children
  renderables.add(canvas)

  privateRenderablesCount += 1

  val permanentRenderables = ListBuffer[Node]()

  /**
   * Lisää uuden renderöitävän hahmon kartalle
   * @param rendr renderöitävä hahmo
   * @param pos renderöitävän hahmon sijainti kartalla
   */
  def addRenderable(rendr: Node, pos: Coordinates): Unit =
    rendr.setLayoutX(pos.x)
    rendr.setLayoutY(pos.y)
    renderables.add(rendr)

  /**
   * Lisää uuden renderöitävän hahmon, mutta ei aseta sen sijaintia
   * @param rendr renderöitävä hahmo
   */
  def addRenderable(rendr: Node): Unit =
    renderables.add(rendr)

  /**
   * Pysyvä renderöitävä asia, sitä ei poisteta, kun kutsutaan .reset() -metodia
   * @param rendr pysyvä renderöitävä hahmo
   * @param pos sijainti, johon renderöidään
   */
  def addPermanent(rendr: Node, pos: Coordinates): Unit =
    rendr.setLayoutX(pos.x)
    rendr.setLayoutY(pos.y)
    renderables.insert(privateRenderablesCount, rendr)
    permanentRenderables += rendr
    privateRenderablesCount += 1

  /**
   * Poistetaan annettu renderöitävä asia pois pysyvistä renderöitävistä
   * @param rendr hahmo, joka halutaan poistaa renderöitävistä
   */
  def removePerm(rendr: Node): Unit =
    val idx = renderables.indexOf(rendr)
    if idx >= 0 then
      renderables.remove(idx)
      permanentRenderables -= rendr
      privateRenderablesCount -= 1

  def clearPerm(): Unit =
    permanentRenderables.clear()
    privateRenderablesCount = 1

  /**
   * Rakennetaan vihollisten tie
   * @param route funktio, joka antaa tien pisteet
   * @param roadWidth tien paksuus
   */
  def constructRoad(route: PolynomialSplineFunction, roadWidth: Int): Unit =
    val derivatives = route.derivative()
    // nimetön funktio luomaan sarja x-akselin pisteitä
    val rangesInclusive = (start: Int, end: Int, steps: Int) => (0 to steps).map( start + _ * ((end - start) / steps.toDouble) ).map( math.min(end, _) )
    val roadPoints = rangesInclusive(route.getKnots.head.toInt, route.getKnots.last.toInt, 5000).map( x => Coordinates(x, route.value(x)) )

    val oldFillColor = g.fill
    g.fill = Orange
    // piirretään tie, ensin määritellään jokaiselle pisteelle oma kulmakerroin
    roadPoints.foreach( point =>
        g.save()
        //g.rotate(0)
        val ok = toDegrees(atan(derivatives.value(point.x)))
        //val rotation = new Rotate(ok, point.x, point.y)
        g.translate(point.x, point.y)
        g.rotate(ok)
        g.fillRect(0, 0, 2, roadWidth * 2)
        g.restore()
      )

    g.fill = oldFillColor
    g.translate(0, 0)


  /**
   * Poistaa kaikki muut piirrettävät objektit
   * kartalta paitsi itse taustan
   */
  def reset() =
    if renderables.size() > 1 then
      privateRenderablesCount = 1
      permanentRenderables.clear()
      renderables.remove(privateRenderablesCount, renderables.size())
  
  def getRoot = map