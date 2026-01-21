package backend.enemy


import backend.ammunition.{Ammunition, Bullet, ExplosiveBullet, FollowingBullet}
import backend.mapHandling.{Coordinates, RouteHandler}
import backend.jsonModels.{AdvancedEnemySerializer, BasicEnemyDeserialized, BasicEnemySerializer, ReinforcedSpawnerDeserialized, Serializer, SpawnerEnemyDeserialized}

import scala.collection.mutable
import scala.collection.mutable.Buffer


enum DeathVariants:
  case BaseDeath(value: Int)
  case SpawnerDeath(value: Int, spawnedEnemies: Buffer[Enemy])
  case OutOfBoundsDeath(remainingHp: Int)


/**
 * Kaikkien vihollisten perus piirreluokka. Kaikki viholliset perivät tämän,
 * ja täydentävät metodeja
 */
sealed trait  Enemy:

  protected var outOfRange = false // käytetään tarkistamaan, että kuoliko vihollinen sen vuoksi, että se kulki ulos kartalta
  protected val camouflaged: Boolean = false

  def toJson: Serializer
  def move(): Unit
  def takeHit(bullet: Ammunition): Unit
  def death(): DeathVariants

  def id: String
  def setOutOfRange(isOut: Boolean): Unit = outOfRange = isOut
  def getHp: Int
  def getSpeed: Int
  def getLocation: Coordinates
  def getCamouflaged: Boolean = camouflaged
  def isDead: Boolean

end Enemy



/**
 * Sisältää kaikkien vihollisluokkien perustoiminnot, kuten liikkumisen sekä sen, että onko
 * vihollinen kuollut
 */
trait Basic(
  protected var hp: Int = 0,
  protected var speed: Int,
  protected val value: Int = 0,
  protected val location: Coordinates = Coordinates(0.0, 0.0)
  )(using route: RouteHandler)
  extends Enemy:

  // käytetään määrittelemään liikerata
  var directionVec: Option[Coordinates] = Some(Coordinates(0.0, 0.0))

  /**
   * Liikuttaa vihollista eteenpäin reitillä
   */
  final def move(): Unit =
    val nextLocation = computeDirection

    nextLocation match
      case Some(nextLocation) =>
        location.x = nextLocation.x
        location.y = nextLocation.y

      case None => outOfRange = true


  override def takeHit(bullet: Ammunition): Unit =
    bullet match
      case a: Bullet =>
        hp -= a.dmg.toInt
        a.penetrationRate -= 1
      case b: ExplosiveBullet =>
        hp -= b.dmg.toInt
        b.penetrationRate -= 1
      case c: FollowingBullet =>
        hp -= c.dmg.toInt
        c.penetrationRate -= 1

  /**
   * Määritetään seuraava piste, johon vihollinen etenee
   * @return Jos on mahdollinen uusi piste, niin antaa Some-olion, muulloin None
   */
  private final def computeDirection: Option[Coordinates] =
    val nextPoint = route.nextLocation(location.x, speed)

    nextPoint

  override def getHp: Int = hp
  override def getSpeed: Int = speed
  override def getLocation: Coordinates = location.copy()
  override def isDead: Boolean = hp <= 0 || outOfRange

  override def death(): DeathVariants =
    if outOfRange then
      DeathVariants.OutOfBoundsDeath(getHp)
    else
      DeathVariants.BaseDeath(value)

end Basic


/**
 * Piirreluokka antaa tarvittavat metodit vihollisille, jotka tuhoutuessaan luovat uusia vihollisia
 */
trait Advanced(val spawnables: Buffer[String], val enemyFactory: EnemyFactory, val value: Int)(using route: RouteHandler) extends Enemy:

  def spawnNewEnemies: mutable.Buffer[Enemy] = spawnables.flatMap(enemyFactory.createEnemyById(_, getLocation, getCamouflaged)(using route.copyFull))

  override def death(): DeathVariants =
    if outOfRange then
      DeathVariants.OutOfBoundsDeath(getHp + spawnNewEnemies.foldLeft(0)((total, curr) =>
        curr.setOutOfRange(true)
        val firstDeath = curr.death()

        firstDeath match
          case DeathVariants.OutOfBoundsDeath(firstHP) =>
            total + firstHP

          case _ => total
      ))
    else
      DeathVariants.SpawnerDeath(value, spawnNewEnemies)

end Advanced


/**
 * Muunnos Basic-piirreluokasta, joka rajaa oliota.
 * Tämä piirreluokka määrittää rajoitetumman määrän ammuksia, jotka tehoavat viholliseen
 */
trait Reinforced(val resistance: Int) extends Basic:

  override def takeHit(bullet: Ammunition): Unit =
    bullet match
      case e: Bullet => e.penetrationRate = 0
      case e: ExplosiveBullet =>
        if e.penetrationRate > resistance then
          hp -= e.dmg.toInt
          e.penetrationRate -= 1
        else
          e.penetrationRate = 0
      case e: FollowingBullet => e.penetrationRate = 0

end Reinforced


/**
 * Vihollinen, joka tuhoutuessan luo uusia vihollisia
 * @param deserialized Case class -malli, joka sisältää tarvittavie tietoja olion luomiseen
 * @param location vihollisen aloitussijainti kartalla
 * @param camouflaged onko vihollinen maastoutunut
 * @param enemyFactory tehdasolio, joka luo uudet viholliset
 * @param route reitti, jota vihollinen kulkee
 */
class SpawnerEnemy(
  deserialized: SpawnerEnemyDeserialized,
  location: Coordinates,
  override val camouflaged: Boolean,
  enemyFactory: EnemyFactory
  )(using route: RouteHandler)
  extends Basic(deserialized.hp, deserialized.speed, deserialized.value, location)
    with Advanced(deserialized.spawnablesId, enemyFactory, deserialized.value):

  val id: String = deserialized.id
  override val value = deserialized.value

  override def toJson = AdvancedEnemySerializer(id, hp, speed, location, camouflaged)

end SpawnerEnemy



class ReinForcedSpawner(
 deserialized: ReinforcedSpawnerDeserialized,
 location: Coordinates,
 override val camouflaged: Boolean,
 enemyFactory: EnemyFactory
 )(using route: RouteHandler)
  extends Reinforced(deserialized.resistance)
    with Basic(deserialized.hp, deserialized.speed, deserialized.value, location)
    with Advanced(deserialized.spawnablesId, enemyFactory, deserialized.value):

  val id: String = deserialized.id
  override val value = deserialized.value

  override def toJson = AdvancedEnemySerializer(id, hp, speed, location, camouflaged)

end ReinForcedSpawner


/**
 * Perus vihollisluokka, ei ole erikoisia ominaisuuksia
 * @param deserialized Case class -malli, joka sisältää tarvittavie tietoja olion luomiseen
 * @param location vihollisen aloitussijainti kartalla
 * @param camouflaged onko vihollinen maastoutunut
 * @param route reitti, jota vihollinen kulkee
 */
class BasicEnemy(deserialized: BasicEnemyDeserialized, location: Coordinates, override val camouflaged: Boolean)(using route: RouteHandler)
  extends Basic(deserialized.hp, deserialized.speed, deserialized.value, location):

  val id: String = deserialized.id
  override val value = deserialized.value

  override def toJson = BasicEnemySerializer(id, hp, speed, location, camouflaged)

end BasicEnemy
