package frontend

import backend.Game
import frontend.gameSession.GameRenderer
import frontend.startPage.{MenuButton, StartPageRenderer}
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import os.Path
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control.Label
import scalafx.scene.layout.Pane
import scalafx.scene.media.{Media, MediaPlayer, MediaView}
import scalafx.scene.paint.Color.{Black, White}
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.Font

/**
 * Juuriluokka, jonka sisältämä scalafx-solmu annetaan sovellukselle renderöitäväksi.
 * @param width ikkunan leveys
 * @param height ikkunan korkeus
 * @param assetsDir osoitelähde, joka sisältää erilaiset materiaalit, kuten hahmojen kuvat ja musiikin
 */
class RootNode(private val width: Int, private val height: Int, private val assetsDir: Path):

  private val rootUIComponent = new Pane():
    pickOnBounds = true
    onMouseClicked = (me) =>
      me.getPickResult.getIntersectedNode match
        // jos painetaan jotain kuvaa, niin täytyy kutsua sen
        // tapahtumia, muulloin käsitellään koko kartan MouseEvent
        case iv: javafx.scene.image.ImageView => iv.fireEvent(me.copyFor(iv, iv))
        case rect: javafx.scene.shape.Rectangle => rect.fireEvent(me.copyFor(rect, rect))
        case _ =>

  val renderables: ObservableBuffer[Node] = rootUIComponent.children


  private val continueGameAction: MouseEvent => Unit = (e: javafx.scene.input.MouseEvent) =>
    try
      val savedGame = Game.startSavedGame()
      val mainUI = GameRenderer(width, height, savedGame, endGameAction, assetsDir)
      val root = mainUI.getRoot

      tickFunction = mainUI.tick
      renderables.clear()
      renderables += root
      renderables ++= toggleMusicButton.getRoot
      e.consume()

    catch
      case err: Exception =>
        val text = s"When trying to start a saved game the app encountered an error: \n\n${err.getMessage}"
        showErrorPage(text)
        e.consume()

  // tätä kutsutaan kun lopetetaan peli ja mennään takaisin aloitussivulle
  private val endGameAction: MouseEvent => Unit = (e: javafx.scene.input.MouseEvent) =>
    try
      val startPageUI = StartPageRenderer(width, height, startGameAction, continueGameAction, assetsDir)
      val root = startPageUI.getRoot

      tickFunction = () => ()
      renderables.clear()
      renderables += root
      renderables ++= toggleMusicButton.getRoot
      e.consume()

    catch
      case err: Exception =>
        val text = s"when trying to go to the main screen the app encountered an error: \n\n${err.getMessage}"
        showErrorPage(text)
        e.consume()

  // funktio, jota kutsutaan aloittamaan peli.
  private val startGameAction: MouseEvent => Unit = (e: javafx.scene.input.MouseEvent) =>
    try
      val buttonId = e.getSource.asInstanceOf[javafx.scene.Node].getId
      val game = Game(buttonId, width = width, height = height)
      val mainUI = GameRenderer(width, height, game, endGameAction, assetsDir)
      val root = mainUI.getRoot

      tickFunction = mainUI.tick
      renderables.clear()
      renderables += root
      renderables ++= toggleMusicButton.getRoot
      e.consume()

    catch
      case err: Exception =>
        val text = s"When trying to start a new game the app encountered an error: \n\n${err.getMessage}"
        showErrorPage(text)
        e.consume()


  private var tickFunction: () => Unit = () => ()


  val startPageUI = StartPageRenderer(width, height, startGameAction, continueGameAction, assetsDir)
  private val root = startPageUI.getRoot


  // muuttujat, joilla saadaan soitettua musiikkia
  private val musicURI = (assetsDir / "music").toIO.toURI.toString
  private val musicName = "Strike_Force_Heroes_OST_Main_Menu_Theme"
  private val audio = new Media(s"$musicURI/${musicName}.mp3")
  private val mediaPlayer = new MediaPlayer(audio)
  mediaPlayer.volume = 50
  private var isPlaying = false

  val mediaView = new MediaView(mediaPlayer)
  // -----------------------------------------

  private val toggleMusicButton = MenuButton(
    width - 175,
    height - 100,
    150,
    50,
    "toggle music",
    clickAction = (e: javafx.scene.input.MouseEvent) =>
      if isPlaying then
        mediaPlayer.stop()
        isPlaying = false
      else
        mediaPlayer.play()
        isPlaying = true
  )

  // lisätään muös musiikki renderöitäviin solmuihin
  renderables += root
  renderables ++= toggleMusicButton.getRoot


  mediaPlayer.cycleCount = Int.MaxValue
  mediaPlayer.play()
  isPlaying = true


  def tick() =
    try
      tickFunction()

    catch
      case e: Exception =>
        val text = s"When running the game the app encountered an error: \n\n${e.getMessage}"
        showErrorPage(text)



  def showErrorPage(errMessage: String): Unit =
    renderables.remove(1, renderables.length - 1)

    val textBackground = Rectangle(0, 0, width, height)
    textBackground.opacity = 0.6
    textBackground.fill = White
    textBackground.mouseTransparent = true


    val text: String = errMessage

    val textUI = Label(text)
    textUI.setFont(Font(25))
    textUI.setTextFill(Black)
    textUI.setMaxWidth(width - 100)
    textUI.setWrapText(true)
    textUI.setPadding(Insets(10))

    val mainMenuButton = MenuButton(width - 350, height - 100, 150, 50, "to main menu",
      clickAction = (e: javafx.scene.input.MouseEvent) =>
        renderables.remove(1, renderables.length - 1)
        endGameAction(e)
        e.consume()
    )

    renderables += textBackground
    renderables += textUI
    renderables ++= mainMenuButton.getRoot
    renderables ++= toggleMusicButton.getRoot


  def getRoot = rootUIComponent

end RootNode
