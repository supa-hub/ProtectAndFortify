
import frontend.RootNode
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import javafx.animation.AnimationTimer


object ProtectAndFortify extends JFXApp3:
  // cannot use "width" and "height" because of naming conflicts
  val widthVal = 1300
  val heightVal = 700

  private val assetsDir = os.pwd / "src" / "main" / "scala" / "assets"

  def start(): Unit =
    stage = new JFXApp3.PrimaryStage:
      title = "ProtectAndFortify"
      width = widthVal
      height = heightVal

    // pääsolmu, joka sisältää kaikki muut sovelluksen osat
    val mainUI = RootNode(widthVal, heightVal, assetsDir)
    val root = mainUI.getRoot

    val scene = Scene(parent = root)
    stage.scene = scene

    // ajastin on asetettu 120fps:ään
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

    // Jos mikään aiempi try - catch osio ei saanut erroria kiinni, niin
    // tämä on viimeinen paikka, joka saa sen
    Thread.currentThread().setUncaughtExceptionHandler((thread, ex) =>
      val text = s"When running the game the app encountered an error: \n\n${ex.getMessage}"
      mainUI.showErrorPage(text)
    )

  end start
