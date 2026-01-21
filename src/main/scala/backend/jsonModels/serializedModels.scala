package backend.jsonModels

import backend.*
import backend.mapHandling.Coordinates
import backend.tower.Boost
import upickle.default.ReadWriter

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Seuraavaa piirreluokkaa ja case classeja käytetään lataamaan ja tallentamaan tasossa olevien hahmojen tiedot,
 */
sealed trait Serializer derives ReadWriter

case class BasicEnemySerializer(id: String, hp: Int, speed: Int, location: Coordinates, camouflaged: Boolean) extends Serializer
case class AdvancedEnemySerializer(id: String, hp: Int, speed: Int, location: Coordinates, camouflaged: Boolean) extends Serializer
case class ReinforcedEnemySerializer(id: String, hp: Int, speed: Int, location: Coordinates, camouflaged: Boolean) extends Serializer

case class BulletSerializer(id: String, dmg: Double, speed: Int, startPos: Coordinates, directionVec: Coordinates, penetrationRate: Int) extends Serializer
case class ExplosiveBulletSerializer(id: String, dmg: Double, speed: Int, startPos: Coordinates, directionVec: Coordinates, penetrationRate: Int, explosionRadius: Int) extends Serializer
case class FollowingBulletSerializer(id: String, dmg: Double, speed: Int, startPos: Coordinates, directionVec: Coordinates, penetrationRate: Int) extends Serializer
case class FollowingExplosiveBulletSerializer(id: String, dmg: Double, speed: Int, startPos: Coordinates, directionVec: Coordinates, penetrationRate: Int, explosionRadius: Int) extends Serializer

case class BasicTowerSerializer(id: String, location: Coordinates, boosts: mutable.Set[Boost]) extends Serializer
case class BoostingTowerSerializer(id: String, location: Coordinates, boosts: mutable.Set[Boost], boughtBoosts: ArrayBuffer[Boost]) extends Serializer


case class GameSerializer(
  enemies: List[Serializer],
  towers: List[Serializer],
  ammunition: List[Serializer],
  mapId: String,
  width: Int,
  height: Int,
  playerHP: Int,
  playerMoney: Int,
  levelStartDelay: Int,
  levelIdx: Int
) derives ReadWriter
