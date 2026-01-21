package backend.enemy

import backend.jsonModels.*
import backend.mapHandling.{Coordinates, RouteHandler}

import scala.collection.concurrent.TrieMap


/**
 * Tehdasluokka erilaisten vihollisten luontiin
 * @param enemyIdMapping Viite eri vihollistyyppien vase classeihin, jossa avain on vihollistyypin id
 */
class EnemyFactory(private val enemyIdMapping: TrieMap[String, Deserialized]):

  def createEnemy(deserialized: Deserialized, location: Coordinates, camouflaged: Boolean, factory: EnemyFactory = this)(using route: RouteHandler): Option[Enemy] =
    deserialized match
      case basic:      BasicEnemyDeserialized        => Some( BasicEnemy(basic, location.copy(), camouflaged) )
      case advanced:   SpawnerEnemyDeserialized      => Some( SpawnerEnemy(advanced, location.copy(), camouflaged, factory) )
      case reinforced: ReinforcedSpawnerDeserialized => Some( ReinForcedSpawner(reinforced, location.copy(), camouflaged, factory) )
      case _ => None

  /**
   * Luodaan oma metodi joka luo viholliset, jotka oli tallennettu olemassa olevaan peliin
   * @param serialized  vihollinen, joka oli tallennettu
   * @param route  vihollisen rata
   * @return palauttaa vihollisen, jos voitiin luoda, muulloin palauttaa None
   */
  def createEnemyBySerializer(serialized: Serializer)(using route: RouteHandler): Option[Enemy] =
    serialized match
      case basic:      BasicEnemySerializer =>
        enemyIdMapping.get(basic.id) match
          case Some(basicDeserialized: BasicEnemyDeserialized) => createEnemy(BasicEnemyDeserialized(basic.id, basic.hp, basic.speed, basicDeserialized.value), basic.location.copy(), basic.camouflaged)
          case _ => None

      case advanced:   AdvancedEnemySerializer =>
        enemyIdMapping.get(advanced.id) match
          case Some(spawnerDeserialized: SpawnerEnemyDeserialized) => createEnemy(SpawnerEnemyDeserialized(advanced.id, advanced.hp, advanced.speed, spawnerDeserialized.spawnablesId, spawnerDeserialized.value), advanced.location.copy(), advanced.camouflaged)
          case _ => None

      case reinforced: ReinforcedEnemySerializer =>
        enemyIdMapping.get(reinforced.id) match
          case Some(reinforcedDeserialized: ReinforcedSpawnerDeserialized) => createEnemy(ReinforcedSpawnerDeserialized(reinforced.id, reinforced.hp, reinforced.speed, reinforcedDeserialized.resistance, reinforcedDeserialized.spawnablesId, reinforcedDeserialized.value), reinforced.location.copy(), reinforced.camouflaged)
          case _ => None
      case _ => None


  def createEnemyById(enemyId: String, location: Coordinates, camouflaged: Boolean)(using route: RouteHandler): Option[Enemy] =
    enemyIdMapping.get(enemyId) match
      case Some(en: Deserialized) => createEnemy(en, location.copy(), camouflaged)
      case None => None

end EnemyFactory