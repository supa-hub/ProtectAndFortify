package frontend.gameSession

import scalafx.scene.image.{Image, ImageView}
import javafx.scene.input.MouseEvent


/**
 * funktio, jolla voi luoda kuvan annetulla tiedoston osoitteella ja nimellä
 * @param imagePath sijainti, jossa tiedosto sijaitsee
 * @param fileName tiedoston nimi
 * @return uusi ImageView -olio, joka sisältää kuvan
 */
def constructObjectUI(imagePath: String, fileName: String, rotation: Double = 0, onClick: MouseEvent => Unit = (_: MouseEvent) => ()): ImageView =
  new ImageView(Image(s"$imagePath/${fileName}.png")):
    fitHeight = 100
    preserveRatio = true
    id = fileName
    onMouseClicked = (e: MouseEvent) => onClick(e)
    rotate = rotation
