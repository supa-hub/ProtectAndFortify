package frontend.gameSession.gridComponents

import backend.tower.{Boost, Tower}
import frontend.startPage.MenuButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.scene.canvas.Canvas
import scalafx.scene.layout.{HBox, Pane, StackPane}
import scalafx.scene.paint.Color.{Black, Blue, Green, Red}
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.Font

import scala.collection.mutable


/**
 * Pelisession alarivi, joka näyttää pelaajan rahat, hp:n, pelin tason sekä painetun BoostingTower -olion
 * myytävänä olevat boostit
 * @param mapWidth alarivin leveys
 * @param mapHeight alarivin korkeus
 * @param playerHP pelaajan alkuhp
 * @param playerMoney pelaajan alkuraha
 */
class BottomRowUI(
  private val mapWidth: Int,
  private val mapHeight: Int,
  var playerHP: Int,
  var playerMoney: Int
  ):
  private var levelCount = 1
  private var permanentRenderables = 1

  // piirtää backroundin
  private val canvas = Canvas(mapWidth, mapHeight)
  canvas.clip = new Rectangle:
    width = mapWidth
    height = mapHeight

  // Getting the GraphicsContext
  private val g = canvas.graphicsContext2D

  //Simple drawing.
  g.fill = Black // Set the fill color.
  g.fillRect(0, 0, mapWidth, mapHeight) // Fill rectangle at (0, 0) with width 600 and mapHeight 450.
  g.fill = Red
  g.font = Font(50) // Set text size
  g.fillText(s"$playerHP", 10, 100)

  val sellButton = MenuButton(mapWidth - 150, mapHeight - 100, 100, 50, "sell: 0")
  private var sellButtonIdx = -1

  // sisältää kaikki piirrettävät elementit.
  private val map = Pane()
  map.clip = new Rectangle:
    width = mapWidth
    height = mapHeight


  map.maxWidth = mapWidth
  map.maxHeight = mapHeight
  map.minWidth = mapWidth
  map.minHeight = mapHeight
  map.prefWidth = mapWidth
  map.prefHeight = mapHeight

  private val renderables = map.children
  renderables.add(canvas)

  var boostMappings: mutable.Map[Boost, Int] = mutable.Map()
  var boostButtons: mutable.Buffer[MenuButton] = mutable.Buffer()


  def updateHP(newHP: Int) =
    playerHP = newHP
    update()


  def updateMoney(newAmount: Int) =
    playerMoney = newAmount
    update()

  def updateLevel(newLevel: Int) =
    levelCount = newLevel
    update()


  def update() =
    g.save()
    g.fill = Black // Set the fill color.
    g.fillRect(0, 0, mapWidth, mapHeight)
    g.fill = Red
    g.font = Font(50) // Set text size
    g.fillText(s"$playerHP", 10, 60)
    g.fill = Green
    g.fillText(s"$playerMoney", 10, 120)
    g.fill = Blue
    g.fillText(s"level: ${levelCount}", 120, 60)
    g.restore()

  /**
   * Näyttää painetun BoostingTower -olion myytävänä olevat boostit
   * @param clickEvent tapahtuma, joka tehdään, kun pelaaja painaa yhtä boostia
   * @param boosts myytävänä olevat boostit
   */
  def showBoosts(clickEvent: (MouseEvent, Boost) => Unit, boosts: mutable.Map[Boost, Int]) =
    boostMappings = boosts

    boostButtons = boostMappings.map( aMapping =>
      val boostButton = MenuButton(
        0, 0,
        170, 50,
        s"${aMapping._1.id}: ${aMapping._2}",
      )

      boostButton.getRootScala.head.onMouseClicked =
        (e: MouseEvent) =>
          clickEvent(e, aMapping._1)
          if boostMappings.size < boostButtons.size then
            boostButtons -= boostButton
          removeBoostDisplay()
          updateBoostButtons()
          e.consume()

      boostButton
    ).toBuffer

    updateBoostButtons()


  def updateBoostButtons() =
    // GUI osat, joilla näytetään boostit, jotka pelaaja voi ostaa
    // -----------------------------------------------
    val buttonRows: Array[VBox] = boostButtons.grouped(2)
      .map {
        case mutable.Buffer(first: MenuButton, second: MenuButton) =>
          val first1 = new StackPane:
            children = first.getRootScala
            pickOnBounds = true
            onMouseClicked = (me) =>
              me.getPickResult.getIntersectedNode match
                // jos painetaan jotain kuvaa, niin täytyy kutsua sen
                // tapahtumia, muulloin käsitellään koko kartan MouseEvent
                case iv: javafx.scene.image.ImageView => iv.fireEvent(me.copyFor(iv, iv))
                case rect: javafx.scene.shape.Rectangle => rect.fireEvent(me.copyFor(rect, rect))
                case _ =>


          val second1 = new StackPane:
            children = second.getRootScala
            pickOnBounds = true
              onMouseClicked = (me) =>
                me.getPickResult.getIntersectedNode match
                  // jos painetaan jotain kuvaa, niin täytyy kutsua sen
                  // tapahtumia, muulloin käsitellään koko kartan MouseEvent
                  case iv: javafx.scene.image.ImageView => iv.fireEvent(me.copyFor(iv, iv))
                  case rect: javafx.scene.shape.Rectangle => rect.fireEvent(me.copyFor(rect, rect))
                  case _ =>


          new VBox(10, first1, second1)

        case mutable.Buffer(first: MenuButton) =>
          val first1 = new StackPane:
            children = first.getRootScala
            pickOnBounds = true
              onMouseClicked = (me) =>
                me.getPickResult.getIntersectedNode match
                  // jos painetaan jotain kuvaa, niin täytyy kutsua sen
                  // tapahtumia, muulloin käsitellään koko kartan MouseEvent
                  case iv: javafx.scene.image.ImageView => iv.fireEvent(me.copyFor(iv, iv))
                  case rect: javafx.scene.shape.Rectangle => rect.fireEvent(me.copyFor(rect, rect))
                  case _ =>

          new VBox(10, first1)

        case _ => new VBox(10.0)
      }.toArray

    val allButtons = new HBox:
      layoutX = 300
      padding  = Insets(10)
      spacing = 10
      children = buttonRows.map(scalafx.scene.layout.VBox(_))

    renderables.add( allButtons )

    allButtons.setPrefWidth(mapWidth - allButtons.width.toDouble - 150 * scala.math.max(buttonRows.length, 3))
    allButtons.setPrefHeight(mapHeight)
    // ----------------


  def removeBoostDisplay() =
    if renderables.size() > 1 then
      renderables.remove(permanentRenderables, renderables.size() - permanentRenderables)


  def showSellButton(tower: Tower, amount: Int) =
    sellButton.setText( s"sell: $amount" )
    sellButton.setId(tower.toString)

    if sellButtonIdx == -1 then
      sellButtonIdx = permanentRenderables
      sellButton.getRoot.foreach( aNode =>
        renderables.insert(permanentRenderables, aNode)
        permanentRenderables += 1
      )

  def removeSellButton() =
    if sellButtonIdx > -1 then
      renderables.remove(sellButtonIdx, 2)

      permanentRenderables -= 2
      sellButtonIdx = -1


  def getRoot = map

end BottomRowUI