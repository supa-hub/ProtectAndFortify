package backend.tower

import backend.{Game, Range}
import backend.ammunition.Ammunition
import backend.jsonModels.*
import backend.mapHandling.Coordinates
import backend.tower.*

import scala.collection.concurrent.TrieMap


class TowerFactory(private val towerIdMapping: TrieMap[String, TowerDeserialized]):

  /**
   * Luo tornin annetusta Deserialized -tyypistä
   * @param deserialized TowerDeserialized, joka vastaa jotain tornityyppiä
   * @param location sijainti, johon torni sijoitetaan
   * @param game peli, johon torni sijoitetaan
   * @return  palauttaa tornin, joka luotiin, jos deserialized ei ole yksi määritellyistä Towerdeserialized tyypeistä, niin palauttaa None
   */
  def createTower(deserialized: TowerDeserialized, location: Coordinates, game: Game): Option[Tower] =
    deserialized match
      case a: BasicTowerDeserialized =>
        // tarkistetaan, että saatiin ammus
        game.bulletFactory.createAmmunitionById(a.bulletId, location.copy()) match
          case Some(bullet: Ammunition) => Some( BasicTower(game, a.id, a.tickPerShot, a.value, location.copy(), bullet, Range(a.visionRadius, location)) )
          case _ => throw Exception(s"The ammo the game tried to create for the tower of id: ${a.id} was not valid.")

      case b: BoostingTowerDeserialized => Some( BoostingTower(game, b.id, b.value, location.copy(), b.possibleBoostsAndPrice.clone(), Range(b.visionRadius, location)) )

      case _ => None


  def createTowerById(id: String, location: Coordinates, game: Game): Option[Tower] =
    towerIdMapping.get(id) match
      case Some(en: TowerDeserialized) => createTower(en, location.copy(), game)
      case None => None


  /**
   * Luodaan oma metodi joka luo tornit, jotka oli tallennettu olemassa olevaan peliin
   *
   * @param serialized torni, joka oli tallennettu
   * @return palauttaa vihollisen, jos voitiin luoda, muulloin palauttaa None
   */
  def createTowerBySerializer(serialized: Serializer, game: Game): Option[Tower] =
    serialized match
      case basic: BasicTowerSerializer =>

        towerIdMapping.get(basic.id) match
          case Some(aTower: BasicTowerDeserialized) =>
            createTower(aTower, basic.location.copy(), game) match
              // lisätään boostit
              case Some(tower) =>
                basic.boosts.foreach( tower.addBoost(_) )
                Some(tower)

              case None => None
          case _ => None


      case boosting: BoostingTowerSerializer =>

        towerIdMapping.get(boosting.id) match
          case Some(aTower: BoostingTowerDeserialized) =>
            createTower(aTower, boosting.location.copy(), game) match
              // lisätään boostit
              case Some(tower: BoostingTower) =>
                boosting.boosts.foreach( tower.addBoost(_) )
                boosting.boughtBoosts.foreach(tower.buyBoost(_))
                Some(tower)

              case _ => None
          case _ => None


      case _ => None


end TowerFactory
