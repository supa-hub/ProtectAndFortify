package backend.ammunition

import backend.enemy.Enemy
import backend.jsonModels.*
import backend.mapHandling.Coordinates


/**
 * Perus ammuksen piirreluokka. Täyttää tavalliksen ammuksen ominaisuudet ja kaikki ammukset perivät tämän
 */
sealed trait Ammunition:

  var directionVec: Coordinates = Coordinates(0, 0)
  var dmg: Double = 0.0
  var penetrationRate = 0

  def getPenetrationRate: Int = penetrationRate
  def location: Coordinates

  def computeDirection: Coordinates =
    // saadaan vihollisen sijainti, jossa torni näki sen ensin
    val targetLocation: Coordinates = directionVec.copy()

    val inverseRoot: Double = 1.0 / math.sqrt(targetLocation.x * targetLocation.x + targetLocation.y * targetLocation.y)

    (targetLocation * inverseRoot)

  def move(): Unit
  def toJson: Serializer
  def id: String
  def copyAmmo(): Ammunition
  def isEqual(other: Ammunition): Boolean

end Ammunition


/**
 * Piirreluokka antaa luodeille ominaisuuden seurata vihollista, jolloin ne aina osuvat kohteeseensa
 */
sealed trait Following(private val currLocation: Coordinates) extends Ammunition:

  protected var targetEnemy: Option[Enemy] = None

  final def target: Option[Enemy] = targetEnemy
  final def setTarget(target: Enemy): Unit = targetEnemy = Some(target)

  // koska kohteen sijainti muuttuu ajan myötä, niin ammukselle määritetään funktio, jolla saadaan aina uusi kohde
  override def computeDirection: Coordinates =
    targetEnemy match
      case Some(enemy) if !enemy.isDead =>
        // määrittää vihollisen uuden sijainnin ja seuraa vihollista
        val targetLocation: Coordinates = enemy.getLocation

        val dCoords = targetLocation - currLocation
        val inverseRoot: Double = 1.0 / math.sqrt(dCoords.x * dCoords.x + dCoords.y * dCoords.y)

        // normalisoidaan suuntavektori
        (dCoords * inverseRoot)

      case _ =>
        penetrationRate = 0
        Coordinates(0, 0)

end Following


/**
 * @param initialdmg             kertoo yhden luodin aiheuttaman vahingon
 * @param initialDirectionVec    kertoon luodin suunnan, käytetään myös kääntämään luodin ulkonäkö oikeaan suuntaan käyttöliittymässä
 * @param initialPenetrationRate kertoo kuinka monen vihollisen läpi luoti voi mennä, ennen kuin luoti tuhoutuu. Perusluodeilla on ensin 1 ja
 *                        eri kehitysten kautta sitä voi kasvattaa
 */
class Bullet(
  val id: String,
  val initialdmg: Double,
  var speed: Int,
  var location: Coordinates,
  val initialDirectionVec: Coordinates,
  val initialPenetrationRate: Int)
  extends Ammunition:

  dmg = initialdmg
  directionVec = initialDirectionVec.copy()
  penetrationRate = initialPenetrationRate
  private val initialLocation: Coordinates = location.copy()


  def move(): Unit =
    val newDir = computeDirection
    location += (newDir * speed.toDouble)

  def toJson: Serializer = BulletSerializer(id, dmg, speed, location, directionVec, penetrationRate)

  override def copyAmmo(): Ammunition = Bullet(id, initialdmg, speed, initialLocation.copy(), initialDirectionVec.copy(), initialPenetrationRate)

  override def isEqual(other: Ammunition): Boolean = this == other

end Bullet


class ExplosiveBullet(
  val id: String,
  val initialdmg: Double,
  var speed: Int,
  var location: Coordinates,
  val initialDirectionVec: Coordinates,
  val initialPenetrationRate: Int,
  var explosionRadius: Int
  ) extends Ammunition:

  dmg = initialdmg
  directionVec = initialDirectionVec.copy()
  penetrationRate = initialPenetrationRate
  protected val initialLocation: Coordinates = location.copy()



  def move(): Unit =
    location += (computeDirection * speed.toDouble)


  def toJson: Serializer = ExplosiveBulletSerializer(id, dmg, speed, location, directionVec, penetrationRate, explosionRadius)

  override def copyAmmo(): Ammunition = ExplosiveBullet(id, initialdmg, speed, initialLocation.copy(), initialDirectionVec.copy(), initialPenetrationRate, explosionRadius)

  override def isEqual(other: Ammunition): Boolean = this == other

end ExplosiveBullet



/**
 * @param initialdmg             kertoo yhden luodin aiheuttaman vahingon
 * @param initialDirectionVec    kertoon luodin suunnan, käytetään myös kääntämään luodin ulkonäkö oikeaan suuntaan käyttöliittymässä
 * @param initialPenetrationRate kertoo kuinka monen vihollisen läpi luoti voi mennä, ennen kuin luoti tuhoutuu. Perusluodeilla on ensin 1 ja
 *                        eri kehitysten kautta sitä voi kasvattaa
 */
class FollowingBullet(
  val id: String,
  val initialdmg: Double,
  var speed: Int,
  var location: Coordinates,
  val initialDirectionVec: Coordinates,
  val initialPenetrationRate: Int
  ) extends Ammunition
    with Following(location):

  dmg = initialdmg
  directionVec = initialDirectionVec
  penetrationRate = initialPenetrationRate
  private val initialLocation: Coordinates = location.copy()

  def move(): Unit =
    targetEnemy match
      case Some(enemy) =>
        if enemy.isDead then
          penetrationRate = -1

        else
          val dCoords = computeDirection
          location += (dCoords * speed.toDouble)

      case None =>

  def toJson: Serializer = BulletSerializer(id, dmg, speed, location, directionVec, penetrationRate)

  override def copyAmmo(): Ammunition = FollowingBullet(id, initialdmg, speed, initialLocation.copy(), initialDirectionVec.copy(), initialPenetrationRate)

  override def isEqual(other: Ammunition): Boolean = this == other

end FollowingBullet




class FollowingExplosiveBullet(
  id: String,
  initialdmg: Double,
  speed: Int,
  location: Coordinates,
  initialDirectionVec: Coordinates,
  initialPenetrationRate: Int,
  explosionRadius: Int
) extends ExplosiveBullet(id, initialdmg, speed, location, initialDirectionVec, initialPenetrationRate, explosionRadius)
    with Following(location):


  override def toJson: Serializer = FollowingExplosiveBulletSerializer(id, dmg, speed, location, directionVec, penetrationRate, explosionRadius)

  override def copyAmmo(): Ammunition = FollowingExplosiveBullet(id, initialdmg, speed, initialLocation.copy(), initialDirectionVec.copy(), initialPenetrationRate, explosionRadius)

end FollowingExplosiveBullet



