package backend.jsonModels

import backend.*
import backend.ammunition.Ammunition
import backend.mapHandling.Coordinates
import backend.tower.Boost
import upickle.default.ReadWriter

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Map

/**
 * Tässä tiedostossa, kun luokan nimessä on: <Deserialized>, niin luokan tiedot on luettu json tiedostosta
 * ja tallennettu olioon.
 *
 * Kun taas nimessä on: <Serializer>, niin luokkaa käytetään tallentamaan tiedot tiedostoihin.
 *
 * Pelin luokille täytyy määritellä nämä kaksi eri luokkaa, koska vaikka olion tyyppi olisi sama, niin
 * lukemisessa saadaan eri tietoja kuin silloin, kun tallennetaan esim. keskeneräinen taso tiedostoon.
 */



/**
 * Seuraavia piirteitä ja case classeja käytetään
 * lataamaan perustiedot eri hahmoista, jotka on tallennettu tiedostoihin.
 */
sealed trait Deserialized derives ReadWriter:
  def id: String
end Deserialized


sealed trait AmmunitionDeserialized extends Deserialized derives ReadWriter

sealed trait TowerDeserialized extends Deserialized derives ReadWriter:
  def value: Int
end TowerDeserialized




/**
 * Seuraavat 3 case classia käytetään saamaan vihollisten perustiedot tiedostoista ja
 * antamaan ne baseData tiedoston olioille.
 * id:tä käytetään tunnistamaan, että minkälaisia vihollisia luodaan esim. muiden viholisten toimesta.
 */
case class BasicEnemyDeserialized(id: String, hp: Int, speed: Int, value: Int) extends Deserialized
case class SpawnerEnemyDeserialized(id: String, hp: Int, speed: Int, spawnablesId: ArrayBuffer[String], value: Int) extends Deserialized
case class ReinforcedSpawnerDeserialized(id: String, hp: Int, speed: Int, resistance: Int, spawnablesId: ArrayBuffer[String], value: Int) extends Deserialized

// tornien case classit
case class BasicTowerDeserialized(id: String, bulletId: String, tickPerShot: Int, value: Int, visionRadius: Int) extends TowerDeserialized
case class BoostingTowerDeserialized(id: String, value: Int, possibleBoostsAndPrice: Map[Boost, Int], visionRadius: Int) extends TowerDeserialized

// eri luotien case classit
case class BulletDeserialized(id: String, dmg: Int, penetrationRate: Int, speed: Int) extends AmmunitionDeserialized
case class ExplosiveBulletDeserialized(id: String, dmg: Int, penetrationRate: Int, speed: Int, explosionRadius: Int) extends AmmunitionDeserialized
case class FollowingBulletDeserialized(id: String, dmg: Int, penetrationRate: Int, speed: Int) extends AmmunitionDeserialized
case class FollowingExplosiveBulletDeserialized(id: String, dmg: Int, penetrationRate: Int, speed: Int, explosionRadius: Int) extends AmmunitionDeserialized

// sisältää vihollisten määrän id -> count muodossa, jossa avain vastaa vihollisen id:tä,
// ja arvo vastaa annetun vihollisen lukumäärää
case class LevelDeserialized(id: String, enemies: Map[String, Int], camouflaged: Boolean) extends Deserialized

case class MapDeserialized(id: String, roadPoints: ArrayBuffer[Coordinates]) extends Deserialized
