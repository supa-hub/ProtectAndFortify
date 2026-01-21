import backend.Game
import frontend.gameSession.GameRenderer

import scalafx.application.JFXApp3
import scalafx.scene.Scene
import javafx.animation.AnimationTimer


object fxBase extends JFXApp3:

  // cannot use "width" and "height" because of naming conflicts
  val widthVal = 1400
  val heightVal = 600

  private val assetsDir = os.pwd / "src" / "main" / "scala" / "assets"

  val game = Game("2", width = widthVal, height = heightVal)


  def start() =
    stage = new JFXApp3.PrimaryStage:
      title = "test"
      width = widthVal
      height = heightVal

    val mainUI = GameRenderer(widthVal, heightVal, game, assetsDir = assetsDir)
    val root = mainUI.getRoot

    val scene = Scene(parent = root)

    stage.scene = scene

    var before: Long = 0
    val delay: Long = 1000000000 / 120

    val timer = new AnimationTimer {
      override def handle(now: Long): Unit = {
        if now - before >= delay then
          mainUI.tick()
          before = now
      }
    }

    timer.start()

  end start
