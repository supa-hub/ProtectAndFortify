package frontend.gameSession

import backend.ammunition.Ammunition
import backend.enemy.Enemy
import backend.tower.Tower

import javafx.scene.input.MouseEvent
import scalafx.scene.image.ImageView
import scala.math.{atan, toDegrees}

type Character = Tower | Enemy | Ammunition

sealed trait ObjectGUI:

  val assetsDir = os.pwd / "src" / "main" / "scala" / "assets"
  val enemiesURI = (assetsDir / "enemies").toIO.toURI.toString
  val towersURI = (assetsDir / "towers").toIO.toURI.toString
  val ammunitionURI = (assetsDir / "ammunition").toIO.toURI.toString

  def isActive: Boolean
  def update(): Unit
  def getRoot: ImageView
  def getCharacter: Character

end ObjectGUI



final class TowerGUI(private val tower: Tower, onClick: MouseEvent => Unit = (e: MouseEvent) => e.consume()) extends ObjectGUI:

  private val image = constructObjectUI(towersURI, tower.id, 0, onClick)
  update()

  override def isActive = true

  override def update(): Unit =
    image.setLayoutX(tower.location.x - image.getBoundsInParent.getWidth / 2)
    image.setLayoutY(tower.location.y - image.getBoundsInParent.getHeight / 2)

  override def getRoot = image

  override def getCharacter: Character = tower

end TowerGUI



final class EnemyGUI(private val enemy: Enemy, onClick: MouseEvent => Unit = (e: MouseEvent) => e.consume()) extends ObjectGUI:

  private val image = constructObjectUI(enemiesURI, enemy.id, 0, onClick)
  if enemy.getCamouflaged then
    image.opacity = 0.6

  update()

  override def isActive: Boolean = !enemy.isDead

  override def update(): Unit =
    image.setLayoutX(enemy.getLocation.x - image.getBoundsInParent.getWidth / 2)
    image.setLayoutY(enemy.getLocation.y - image.getBoundsInParent.getHeight / 2)

  override def getRoot = image

  override def getCharacter: Character = enemy

end EnemyGUI


final class AmmunitionGUI(private val ammunition: Ammunition, private val width: Int, private val height: Int, onClick: MouseEvent => Unit = (e: MouseEvent) => e.consume()) extends ObjectGUI:

  private val direction = ammunition.computeDirection
  private val image = constructObjectUI(ammunitionURI, ammunition.id, toDegrees(atan(direction.y / direction.x)), onClick)
  update()

  override def isActive: Boolean = ammunition.location.x > 0 && ammunition.location.y > 0 && ammunition.location.x < width && ammunition.location.y < height && ammunition.penetrationRate > 0

  override def update(): Unit =
    val direction = ammunition.computeDirection
    var directionDegrees = toDegrees(atan(direction.y / direction.x))

    if direction.x < 0 then
      directionDegrees -= 90
    else if direction.x > 0 then
      directionDegrees += 90
    image.rotate = directionDegrees

    image.setLayoutX(ammunition.location.x)
    image.setLayoutY(ammunition.location.y)

  override def getRoot = image

  override def getCharacter: Character = ammunition

end AmmunitionGUI