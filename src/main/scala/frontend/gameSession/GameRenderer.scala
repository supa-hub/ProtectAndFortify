package frontend.gameSession

import backend.Game
import backend.mapHandling.Coordinates
import backend.tower.{Boost, BoostingTower}
import frontend.gameSession.gridComponents.{BottomCornerUI, BottomRowUI, MapRenderer, TowerMenu}
import frontend.startPage.MenuButton

import scalafx.scene.Node
import scalafx.scene.canvas.Canvas
import javafx.scene.input.MouseEvent
import scalafx.scene.layout.{GridPane, Pane, Priority, RowConstraints}
import scalafx.scene.paint.Color.{Black, Blue, Gray}
import scalafx.scene.shape.{Circle, Rectangle}
import scalafx.scene.text.Font
import os.Path

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


class GameRenderer(
  val width: Int,
  val height: Int,
  private val game: Game = Game("1"),
  private val endGameAction: MouseEvent => Unit = (_: MouseEvent) => (),
  private val assetsDir: Path
):

  val gameGUI = GridPane()


  val enemiesURI = (assetsDir / "enemies").toIO.toURI.toString
  val towersURI = (assetsDir / "towers").toIO.toURI.toString
  val ammunitionURI = (assetsDir / "ammunition").toIO.toURI.toString


  /*
   * Seuraavia funktioita ja muuttujaa käytetään
   * tunnistamaan, että onko pelaaja valinnut jonkin tornin, ja
   * halaua sijoittaa sen kartalle.
  */
  var idOfTowerClicked: Option[String] = None

  var lastClickedMapCharacter: Option[Node] = None // viimeisin kartalla oleva hahmo, jota clickattiin

  val checkTowerIsClicked = (towerId: String) =>
    idOfTowerClicked = Some(towerId)

  val mapIsClicked: Coordinates => Unit = (location: Coordinates) =>
    map.clearPerm()
    bottomRow.removeBoostDisplay()
    //bottomRow.removeSellButton()
    idOfTowerClicked match
      case Some(id) =>
        game.placeTower(id, location)
        idOfTowerClicked = None
    // jos mitään tornia ei painettu, niin painettiin karttaa
      case _ =>

  // tätä kutsutaan tallentamaan peli
  private val saveGameAction = (e: javafx.scene.input.MouseEvent) =>
    game.saveGame()
    endGameAction(e)
    e.consume()

  val renderables = ArrayBuffer[ObjectGUI]()

  private val mapURI = (assetsDir / "maps" / "background").toIO.toURI.toString
  private val mapName = "grass_template"
  val map = MapRenderer((width * 3.0/4).toInt, (height * 3.0/4).toInt, mapURI, mapName, mapIsClicked)

  // luodaan tornivalikko
  val towers = TowerMenu(
    (width * 1.0/4).toInt,
    (height * 3.0/4).toInt,
    towersURI, 
    checkTowerIsClicked, 
    game.getFileHandler.towerIdMapping.values
      .map(aTower => (aTower.id, aTower.value))
      .toIndexedSeq
  )

  // alarivi, jossa on lisätietoa
  val bottomRow = BottomRowUI((width * 3.0/4).toInt, (height * 1.0/4).toInt, game.getState.playerHP, game.getState.playerMoney)

  bottomRow.sellButton.setOnClickAction(
    (e: javafx.scene.input.MouseEvent) =>
      val buttonId = e.getSource.asInstanceOf[javafx.scene.Node].getId
      val wasSold = game.sellTowerByIdInstance(buttonId)
      bottomRow.removeSellButton()
      bottomRow.removeBoostDisplay()
      map.clearPerm()
      e.consume()
  )


  val endGameButton = MenuButton(0, 0, 125, 50, "end game", clickAction = endGameAction)
  val saveGameButton = MenuButton(0, 0, 125, 50, "save game", clickAction = saveGameAction)
  val bottomCorner = BottomCornerUI((width * 1.0/4).toInt, (height * 1.0/4).toInt, endGameButton, saveGameButton)

  // rajoitetaan manuaalisesti eri GridPane osien laajentumista
  val topRow = new RowConstraints:
    minHeight = 300
    prefHeight = (height * 3.0/4).toInt
    maxHeight = (height * 3.0/4).toInt
    vgrow = Priority.Never


  gameGUI.rowConstraints.addAll(topRow)

  // lisätään komponentit ikkunaan

  gameGUI.add(map.getRoot, 0, 0, 3, 3)
  gameGUI.add(bottomRow.getRoot, 0, 3, 3, 1)
  gameGUI.add(towers.getRoot, 3, 0, 1, 3)
  gameGUI.add(bottomCorner.getRoot, 3, 3, 1, 1)


  // luodaan vihollisten tie
  val routeWidth = game.getState.map.routeWidth
  game.getState.map.routes.foreach(
    map.constructRoad(_, routeWidth)
  )


  // lisätään tallennetun pelin tornit ja viholliset kartalle
  game.getState.towers.foreach(tower =>
      val clickEvent = (e: MouseEvent) =>

        // jos torni on tyyppiä BoostingTower, niin näytetään
        // ostettavat boostit alarivissä
        tower match
          case a: BoostingTower =>

            val whenBoostIsClicked = (_: MouseEvent, aBoost: Boost) =>
              game.buyBoost(a, aBoost)
              //bottomRow.updateBoostButtons()

            bottomRow.removeBoostDisplay()
            bottomRow.showBoosts(whenBoostIsClicked, a.getBoosts)

          case _ =>
        // näytetään ympyrä, johon torni näkee
        map.clearPerm()
        val circle = Circle(0, 0, tower.getVisionRange.radius, Black)
        circle.opacity = 0.5
        circle.mouseTransparent = true
        map.addPermanent(circle, tower.location)

        // näytetään myyntinappi
        bottomRow.showSellButton(tower, (tower.value * 0.8).toInt)
        e.consume()

      val image = TowerGUI(tower, clickEvent)
      renderables += image

      map.addRenderable(
        image.getRoot
      )
  )

  game.getState.enemies.foreach(enemy =>
      val image = EnemyGUI(enemy)
      renderables += image

      map.addRenderable(
        image.getRoot
      )
  )


  def getRoot = gameGUI

  /**
   * Kun peli päättyy, niin piirretään lopetusIkkuna
   * @param text teksti, joka renderöidään
   */
  def renderFinishScreen(text: String) =
    gameGUI.children.clear()

    // sisältää kaikki piirrettävät elementit.
    val finishScreen = new Pane():
      pickOnBounds = true
      onMouseClicked = (me) =>
        me.getPickResult.getIntersectedNode match
          // jos painetaan jotain kuvaa, niin täytyy kutsua sen
          // tapahtumia, muulloin käsitellään koko kartan MouseEvent
          case iv: javafx.scene.image.ImageView => iv.fireEvent(me.copyFor(iv, iv))
          case rect: javafx.scene.shape.Rectangle => rect.fireEvent(me.copyFor(rect, rect))
          case _ =>

    // piirtää backroundin
    val canvas = new Canvas(width, height):
      mouseTransparent = true

    canvas.clip = Rectangle(0, 0, width, height)

    // Getting the GraphicsContext
    val g = canvas.graphicsContext2D

    //Simple drawing.
    g.fill = Gray // Set the fill color.
    g.fillRect(0, 0, width, height) // Fill rectangle at (0, 0) with mapWidth 600 and mapheight 450.
    g.fill = Blue
    g.font = Font(50) // Set text size
    g.fillText(text, width / 2 - 20 * (text.length / 2), height / 2)


    val renderables = finishScreen.children
    renderables.add(canvas)
    renderables ++= MenuButton((width * 0.5).toInt, (height * 0.5).toInt + 100, 125, 50, "end game", clickAction = endGameAction).getRoot

    gameGUI.children = finishScreen

  def showWinningScreen() = renderFinishScreen("You won the emu war!")


  def showLosingScreen() = renderFinishScreen("You lost the war against the emus...  wow.")

  def tick() =
    game.tick()
    renderables.foreach(_.update())

    // jotta ei jatkuvasti kutsuta <getState> -metodia, niin tallennetaan se tässä
    val currState = game.getState

    if !currState.gameFinished then
      renderables.filterInPlace(rendr =>
        currState.towers.contains(rendr.getCharacter)
        || currState.enemies.contains(rendr.getCharacter)
        || currState.ammunition.contains(rendr.getCharacter)
      )

      map.renderables.removeIf(rendr =>
        rendr != map.renderables.head
        && !map.permanentRenderables.contains(rendr)
        && !renderables.exists(_.getRoot == rendr)
      )

      // lisätään pelin tornit, ammukset ja viholliset kartalle
      currState.towers.foreach(tower =>
        renderables.find(_.getCharacter == tower) match
          case Some(tower) => ()
          case None =>
            val clickEvent = (e: MouseEvent) =>
              // jos torni on tyyppiä BoostingTower, niin näytetään
              // ostettavat boostit alarivissä
              tower match
                case a: BoostingTower =>

                  val whenBoostIsClicked = (_: MouseEvent, aBoost: Boost) =>
                    game.buyBoost(a, aBoost)
                    //bottomRow.updateBoostButtons()

                  bottomRow.removeBoostDisplay()
                  bottomRow.showBoosts(whenBoostIsClicked, a.getBoosts)

                case _ =>

              // näytetään alue, johon torni näkee
              map.clearPerm()
              val circle = Circle(0, 0, tower.getVisionRange.radius, Black)
              circle.opacity = 0.5
              circle.mouseTransparent = true
              map.addPermanent(circle, tower.location)

              // näytetään myyntinappi
              bottomRow.showSellButton(tower, (tower.value * 0.8).toInt)
              e.consume()

            val image = TowerGUI(tower, clickEvent)
            renderables += image

            map.addRenderable(
              image.getRoot
            )
      )


      currState.enemies.foreach(enemy =>
        renderables.find(_.getCharacter == enemy) match
          case Some(enemy) => ()
          case None =>
            val image = EnemyGUI(enemy)
            renderables += image

            map.addRenderable(
              image.getRoot
            )
      )

      currState.ammunition.foreach(ammunition =>
        renderables.find(_.getCharacter == ammunition) match
          case Some(tower) => ()
          case None =>

            val image = AmmunitionGUI(ammunition, width, height)
            renderables += image

            val direction = ammunition.computeDirection

            map.addRenderable(
              image.getRoot
            )
      )

      bottomRow.updateHP(currState.playerHP)
      bottomRow.updateMoney(currState.playerMoney)
      bottomRow.updateLevel(currState.level)

    else
      if currState.playerWon then
        showWinningScreen()
      else
        showLosingScreen()

end GameRenderer