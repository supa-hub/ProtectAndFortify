package backend.tower

import backend.ammunition.{Ammunition, Following}
import backend.enemy.Enemy
import backend.mapHandling.Coordinates
import backend.jsonModels.{BasicTowerSerializer, BoostingTowerSerializer, Serializer}
import backend.{Game, Range}

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, Set}


/**
 * Perustornin piirreluokka, jonka kaikki tornityypit perivät
 */
sealed trait Tower(val location: Coordinates, val originalVisionRange: Range, var hasCamoVision: Boolean = false):
  // toimii suuntavektorina, alkuasennossa katsoo ylöspäin
  val direction: Coordinates = Coordinates(0, -1)
  protected val boosts: Set[Boost] = Set()

  def toJson: Serializer
  def id: String
  def getVisionRange: Range
  def value: Int

  def addBoost(boost: Boost): Unit = boosts += boost
  def removeBoost(boost: Boost): Unit = boosts -= boost

  protected inline def applyTowerBoosts(): Unit

end Tower



sealed trait ShootingTower:

  def shoot: Option[Ammunition]
  def detect(enemies: Iterable[Enemy]): Option[Enemy]

  protected inline def applyAmmoBoosts(ammo: Ammunition): Ammunition

end ShootingTower


/**
 *
 * @param game peli johon torni lisätään
 * @param id tornityypin id
 * @param originalTicksPerShot tickien määrä ampumisten välillä
 * @param value tornin arvo
 * @param location sijainti kartalla
 * @param ammo ammus, jota torni ampuu
 * @param visionRange alue, johon torni näkee
 */
class BasicTower(val game: Game, val id: String, val originalTicksPerShot: Int, val value: Int, location: Coordinates, ammo: Ammunition, private var visionRange: Range)
  extends Tower(location, Range(visionRange.radius, location))
    with ShootingTower:

  private val baseAmmo = ammo.copyAmmo()
  private var usingBullet = baseAmmo.copyAmmo()
  private var lastTimeShot = 0
  private var ticksPerShot = originalTicksPerShot

  /**
   * Ampuu yksittäisen ammuksen
   * @return jos torni näkee vihollisen ja on kulunut tarpeeksi aikaa viimeisestä,
   *         niin Some(ammus), muulloin None
   */
  override def shoot: Option[Ammunition] =
    applyTowerBoosts()

    detect(game.getState.enemies) match
      case Some(enemy) =>
        lastTimeShot += 1
        usingBullet = applyAmmoBoosts(usingBullet)
        usingBullet.directionVec = enemy.getLocation - location.copy()

        usingBullet match
          case ammo: Following => ammo.setTarget(enemy)
          case _ =>

        if lastTimeShot % ticksPerShot == 0 then
          Some(usingBullet)
        else
          None

      case None =>
        lastTimeShot = 0
        None


  override def detect(enemies: Iterable[Enemy]): Option[Enemy] =
    enemies.find(
      anEnemy => visionRange.inside(anEnemy.getLocation) && (!anEnemy.getCamouflaged || hasCamoVision)
    )


  override protected inline def applyAmmoBoosts(ammo: Ammunition): Ammunition =
    val aux = ammo.copyAmmo()
    //aux.location = location.copy()
    hasCamoVision = false

    boosts.foreach {
      case DmgMultiplier(_, multiplier) => aux.dmg *= multiplier
      case _ =>
    }

    aux


  override protected inline def applyTowerBoosts(): Unit =
    hasCamoVision = false

    boosts.foreach {
      case CamoVision(_, canSee) => hasCamoVision = canSee
      case RangeBoost(_, multiplier) => visionRange = originalVisionRange * multiplier
      case FireRate(_, multiplier) => ticksPerShot = scala.math.max(1, (originalTicksPerShot / (multiplier)).toInt)
      case _ =>
    }

  override def getVisionRange: Range = visionRange
  override def toJson: Serializer = BasicTowerSerializer(id, location, boosts)

end BasicTower


/**
 * Luokka niille torneille, jotka voivat antaa päivityksiä muille
 * @param game peli johon torni lisätään
 * @param id tornityypin id
 * @param value tornin arvo
 * @param location tornin sijainti kartalla
 * @param possibleBoostsAndPrices päivitykset, jota voidaan ostaa
 * @param visionRange alue, johon torni näkee
 */
class BoostingTower(val game: Game, val id: String, val value: Int, location: Coordinates, private val possibleBoostsAndPrices: mutable.Map[Boost, Int], private var visionRange: Range)
  extends Tower(location, Range(visionRange.radius, location)):

  private val boughtBoosts: ArrayBuffer[Boost] = ArrayBuffer()

  def giveBoosts(): Unit =
    applyTowerBoosts()
    val towersClose = findTowers(game.getState.towers)

    towersClose.foreach( aTower =>
      boughtBoosts.foreach( aTower.addBoost(_) )
    )


  final def getBoosts = possibleBoostsAndPrices

  final def buyBoost(boost: Boost): Unit =
    if possibleBoostsAndPrices.contains(boost) then
      boughtBoosts += boost
      possibleBoostsAndPrices -= boost


  final private def findTowers(towers: Iterable[Tower]): Iterable[Tower] =
    towers.filter(
      aTower => visionRange.inside(aTower.location) && ( aTower != this )
    )

  override def getVisionRange: Range = visionRange

  override def toJson: Serializer = BoostingTowerSerializer(id, location, boosts, boughtBoosts)

  // BoostingToweriin voi vaikuttaa vain RangeBoost
  override protected inline def applyTowerBoosts(): Unit =
    boosts.foreach {
      case RangeBoost(_, multiplier) => visionRange = originalVisionRange * multiplier
      case _ =>
    }

end BoostingTower