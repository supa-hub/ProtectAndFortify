package backend.ammunition

import backend.jsonModels.{AmmunitionDeserialized, BulletDeserialized, BulletSerializer, ExplosiveBulletDeserialized, ExplosiveBulletSerializer, FollowingBulletDeserialized, FollowingBulletSerializer, FollowingExplosiveBulletDeserialized, FollowingExplosiveBulletSerializer, Serializer}
import backend.mapHandling.Coordinates

import scala.collection.concurrent.TrieMap

/*
 * luodaan toiset constructorit tehtaaseen helpottamaan luontia Game -luokassa
*/
class BulletFactory(private val ammunitionIdMapping: TrieMap[String, AmmunitionDeserialized]):

  def createBulletBySerializer(serializer: Serializer): Option[Ammunition] =
    serializer match
      case serializerData: BulletSerializer =>
        Some(Bullet(
          serializerData.id,
          serializerData.dmg,
          serializerData.speed,
          serializerData.startPos,
          serializerData.directionVec,
          serializerData.penetrationRate
        ))

      case serializerData: ExplosiveBulletSerializer =>
        Some(ExplosiveBullet(
          serializerData.id,
          serializerData.dmg,
          serializerData.speed,
          serializerData.startPos,
          serializerData.directionVec,
          serializerData.penetrationRate,
          serializerData.explosionRadius
        ))


      case serializerData: FollowingBulletSerializer =>
        Some(FollowingBullet(
          serializerData.id,
          serializerData.dmg,
          serializerData.speed,
          serializerData.startPos,
          serializerData.directionVec,
          serializerData.penetrationRate
        ))

      case serializerData: FollowingExplosiveBulletSerializer =>
        Some(FollowingExplosiveBullet(
          serializerData.id,
          serializerData.dmg,
          serializerData.speed,
          serializerData.startPos,
          serializerData.directionVec,
          serializerData.penetrationRate,
          serializerData.explosionRadius
        ))

      case _ => None


  def createAmmunition(deserialized: AmmunitionDeserialized, location: Coordinates, direction: Coordinates = Coordinates(0, 0)): Option[Ammunition] =
    deserialized match
      case serializerData: BulletDeserialized =>
        Some(Bullet(
          serializerData.id,
          serializerData.dmg,
          serializerData.speed,
          location,
          direction,
          serializerData.penetrationRate
        ))

      case serializerData: ExplosiveBulletDeserialized =>
        Some(ExplosiveBullet(
          serializerData.id,
          serializerData.dmg,
          serializerData.speed,
          location,
          direction,
          serializerData.penetrationRate,
          serializerData.explosionRadius
        ))

      case serializerData: FollowingBulletDeserialized =>
        Some(FollowingBullet(
          serializerData.id,
          serializerData.dmg,
          serializerData.speed,
          location,
          direction,
          serializerData.penetrationRate
        ))

      case serializerData: FollowingExplosiveBulletDeserialized =>
        Some(FollowingExplosiveBullet(
          serializerData.id,
          serializerData.dmg,
          serializerData.speed,
          location,
          direction,
          serializerData.penetrationRate,
          serializerData.explosionRadius
        ))

      case _ => None

  def createAmmunitionById(id: String, location: Coordinates, direction: Coordinates = Coordinates(0, 0)): Option[Ammunition] =
    ammunitionIdMapping.get(id) match
      case Some(ammo: AmmunitionDeserialized) => createAmmunition(ammo, location.copy(), direction.copy())
      case None => None
      case _ => None

end BulletFactory
