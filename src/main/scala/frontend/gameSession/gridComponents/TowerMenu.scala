package frontend.gameSession.gridComponents


import scalafx.geometry.Insets
import scalafx.scene.control.Label
import scalafx.scene.image.{Image, ImageView}
import scalafx.scene.layout.*
import scalafx.scene.paint.Color
import scalafx.scene.paint.Color.Black
import scalafx.scene.text.Font
import scalafx.scene.Node

import scala.collection.mutable.ArrayBuffer

/**
 * Luo UI valikon, josta pelaaja voi valita, että minkä tornin lisää peliin
 * @param width valikon leveys
 * @param height valikon korkeus
 * @param imagePath osoitelähde, joka sisältää piirrettävien tornien kuvat
 * @param towerIdsAndPrices erityyppisten tornien id:t sekä niitä vastaavat hinnat
 */
class TowerMenu(
   private val width: Int,
   private val height: Int,
   private val imagePath: String,
   private val onClickAction: String => Unit,
   towerIdsAndPrices: IndexedSeq[(String, Int)]
  ):

  val towerRows = towerIdsAndPrices.grouped(2)
    .map(pair =>
      pair.map(aTower =>
        // otetaan ensin tiedostoista tornien ID:tä
        // vastaavat kuvat
        val first = new ImageView(Image(s"$imagePath/${aTower._1}.png")):
          fitHeight = 100
          preserveRatio = true
          id = aTower._1 // asetetaan ID:t kuville



        first.onMouseClicked = (me) =>
          // parametri on javaFX ImageView, jolloin täytyy luoda uusi scalaFX ImageView, josta ottaa id
          val imageId = ImageView(me.getSource.asInstanceOf[javafx.scene.image.ImageView]).id.value
          onClickAction(imageId)
          me.consume()


        val text: String = s"price: ${aTower._2}"

        val textUI = Label(text)
        textUI.setFont(Font(15))
        textUI.setTextFill(Black)
        textUI.setWrapText(true)

        VBox(first, textUI)
      )
    )
    .map {
      case IndexedSeq(first: VBox, second: VBox) => HBox(spacing = 25, first, second)
      case IndexedSeq(first: VBox) => HBox(spacing = 25, first)
      case _ => HBox(spacing = 25)
    }
    .toArray

  // sisältää valikon kaikki UI osat
  private val allTowers = new VBox:
    padding  = Insets(10)
    spacing = 10
    children = towerRows


  allTowers.setBackground(new Background(Array(new BackgroundFill(
    Color.LightBlue,
    CornerRadii.Empty,
    Insets.Empty
  ))))


  allTowers.setPrefWidth(width)
  allTowers.setPrefHeight(height)

  def getRoot = allTowers

end TowerMenu
