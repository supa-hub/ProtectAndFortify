package frontend.gameSession.gridComponents

import frontend.startPage.MenuButton
import scalafx.scene.canvas.Canvas
import scalafx.scene.layout.{HBox, Pane, StackPane}
import javafx.scene.layout.VBox
import scalafx.geometry.Insets
import scalafx.scene.Node
import scalafx.scene.paint.Color.{Black, Red}
import scalafx.scene.shape.Rectangle
import scalafx.scene.text.Font


class BottomCornerUI(
 private val width: Int,
 private val height: Int,
 private val buttons: MenuButton*
):

  // piirtää backroundin
  private val canvas = Canvas(width, height)
  canvas.clip = Rectangle(width = width, height = height)
  canvas.mouseTransparent = true

  // Getting the GraphicsContext
  private val g = canvas.graphicsContext2D

  //Simple drawing.
  g.fill = Black // Set the fill color.
  g.fillRect(0, 0, width, height) // Fill rectangle at (0, 0) with width 600 and mapHeight 450.



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

  map.clip = Rectangle(width = width, height = height)


  val renderables = map.children

  val ok = buttons.flatMap(_.getRoot)

  renderables += canvas
  //renderables ++= buttons.flatMap(_.getRoot)


  val buttonRows: Array[VBox] = buttons.grouped(2)
    .map {
      case IndexedSeq(first: MenuButton, second: MenuButton) =>
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

      case IndexedSeq(first: MenuButton) =>
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

  private val allButtons = new HBox:
    padding  = Insets(10)
    spacing = 10
    children = buttonRows.map(scalafx.scene.layout.VBox(_))

  renderables += allButtons

  allButtons.setPrefWidth(width)
  allButtons.setPrefHeight(height)

  def getRoot = map

end BottomCornerUI


