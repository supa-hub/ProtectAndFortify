package frontend.startPage

import javafx.scene.input.MouseEvent
import scalafx.scene.Node
import scalafx.scene.paint.Color
import scalafx.scene.paint.Color.{Black, White}
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.{Font, Text}

/**
 * Luokka, joka sisältää perusNapin, joka koostuu neliöstä ja tekstistä.
 * @param x x-koordinaatti
 * @param y y-koordinaatti
 * @param width napin leveys
 * @param height napin korkeus
 * @param text tekstin sisältä
 * @param id napin id.
 * @param buttonColor neliön väri
 * @param textColor tekstin väri
 * @param clickAction toiminto, joka suoritetaan, kun nappia painetaan
 */
class MenuButton(
  private val x: Int,
  private val y: Int,
  private val width: Int,
  private val height: Int,
  private val text: String,
  private val id: String = "",
  private val buttonColor: Color = White,
  private val textColor: Color = Black,
  private val clickAction: MouseEvent => Unit = (_: MouseEvent) => ()
):

  val buttonRect = Rectangle(x, y, width, height)
  buttonRect.id = id
  buttonRect.onMouseClicked = (e: MouseEvent) =>
    clickAction(e)
    e.consume()
  buttonRect.fill = buttonColor

  val textGUI = Text(x + ( width / 2 - 11 * (text.length / 2) ), y + height / 2, text)
  textGUI.font = Font(20)
  textGUI.fill = textColor
  textGUI.mouseTransparent = true

  def setText(text: String) = textGUI.text = text

  def setId(id: String) = buttonRect.id = id

  def setOnClickAction(clickAction: MouseEvent => Unit) =
    buttonRect.onMouseClicked = (e: MouseEvent) =>
      clickAction(e)
      e.consume()

  def getRootScala: Seq[Node] = Seq(buttonRect, textGUI)

  def getRoot: Seq[javafx.scene.Node] = Seq(buttonRect, textGUI)

end MenuButton


