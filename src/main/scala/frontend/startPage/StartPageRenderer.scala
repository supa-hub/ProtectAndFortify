package frontend.startPage

import filehandling.FileHandler

import os.{Path, read}
import scalafx.geometry.Insets
import scalafx.scene.canvas.Canvas
import scalafx.scene.control.Label
import scalafx.scene.image.Image
import scalafx.scene.layout.Pane
import scalafx.scene.paint.Color.{Black, LightGreen, White}
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.Font

class StartPageRenderer(
  private val width: Int,
  private val height: Int,
  private val startGameAction: javafx.scene.input.MouseEvent => Unit = (_: javafx.scene.input.MouseEvent) => (),
  private val continueGameAction: javafx.scene.input.MouseEvent => Unit = (_: javafx.scene.input.MouseEvent) => (),
  private val assetsDir: Path,
):

  // sisältää kaikki piirrettävät elementit.
  private val map = new Pane():
    pickOnBounds = true
    onMouseClicked = (me) =>
      me.getPickResult.getIntersectedNode match
        // jos painetaan jotain kuvaa, niin täytyy kutsua sen
        // tapahtumia, muulloin käsitellään koko kartan MouseEvent
        case iv: javafx.scene.image.ImageView => iv.fireEvent(me.copyFor(iv, iv))
        case rect: javafx.scene.shape.Rectangle => rect.fireEvent(me.copyFor(rect, rect))
        case _ =>

  map.clip = Rectangle(0, 0, width, height)


  private val canvas = new Canvas(width, height):
    mouseTransparent = true

  canvas.clip = Rectangle(0, 0, width, height)

  // Getting the GraphicsContext
  private val g = canvas.graphicsContext2D

  g.save()
  g.fill = Black // Set the fill color.
  g.fillRect(0, 0, width, height)
  g.restore()


  private val frontpageURI = (assetsDir / "frontpage").toIO.toURI.toString
  private val imageName = "frontpage"

  g.drawImage(Image(s"$frontpageURI/${imageName}.jpg"), 0, 0, width, height)

  private val fileHandler = FileHandler()


  val renderables = map.children
  renderables.add(canvas)

  drawChooseMapButton()
  drawContinueGameButton()
  drawAboutButton()


  // seuraavat 3 funktiota piirtävät alkunäytön napit
  def drawChooseMapButton() =
    val button = MenuButton(width / 2 - 300 / 2, 100, 200, 65, "New game", buttonColor = LightGreen, clickAction = (_: javafx.scene.input.MouseEvent) => showMaps())

    renderables ++= button.getRoot


  def drawContinueGameButton() =
    val button = MenuButton(width / 2 - 300 / 2, 300, 200, 65, "Continue saved game", buttonColor = LightGreen, clickAction = continueGameAction)

    renderables ++= button.getRoot

  def drawAboutButton() =
    val button = MenuButton(width / 2 - 300 / 2, 500, 200, 65, "About", buttonColor = LightGreen, clickAction = (_: javafx.scene.input.MouseEvent) => showAbout())

    renderables ++= button.getRoot
  //--------------------------------------------------------

  /**
   * Funktio, joka esittää karttavalikoiman
   */
  def showMaps(): Unit =
    renderables.remove(1, renderables.length - 1)

    val mapsIds = fileHandler.loadMaps.map(_.id)

    var XCoord = 100
    var YCoord = 100


    mapsIds.foreach(anId =>
      val map1 = MenuButton(XCoord, YCoord, 100, 100, s"map ${anId}", anId, clickAction = startGameAction)

      renderables ++= map1.getRoot

      XCoord += 200

      if XCoord >= width - 300 then
        XCoord = 100
        YCoord += 200
    )

    val mainMenuButton = MenuButton(width - 350, height - 100, 150, 50, "to main menu",
      clickAction = (e: javafx.scene.input.MouseEvent) =>
        renderables.remove(1, renderables.length - 1)
        drawChooseMapButton()
        drawContinueGameButton()
        drawAboutButton()
    )

    renderables ++= mainMenuButton.getRoot


  /**
   * Esittää näytöllä tekstin, joka luetaan about.txt -tiedostosta.
   */
  def showAbout(): Unit =
    renderables.remove(1, renderables.length - 1)

    val textBackground = Rectangle(0, 0, width, height)
    textBackground.opacity = 0.6
    textBackground.fill = White

    val aboutTextURI = assetsDir / "frontpage"
    val aboutTextName = "about.txt"

    val text: String = read(aboutTextURI / aboutTextName)

    val textUI = Label(text)
    textUI.setFont(Font(25))
    textUI.setTextFill(Black)
    textUI.setMaxWidth(width - 100)
    textUI.setWrapText(true)
    textUI.setPadding(Insets(10))

    val mainMenuButton = MenuButton(width - 350, height - 100, 150, 50, "to main menu",
      clickAction = (e: javafx.scene.input.MouseEvent) =>
        renderables.remove(1, renderables.length - 1)
        drawChooseMapButton()
        drawContinueGameButton()
        drawAboutButton()
    )

    renderables += textBackground
    renderables += textUI
    renderables ++= mainMenuButton.getRoot


  def getRoot = map

end StartPageRenderer
