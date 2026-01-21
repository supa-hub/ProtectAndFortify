import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable.{ArrayBuffer, Map}
import backend.jsonModels.{BasicEnemyDeserialized, BasicTowerDeserialized, BulletDeserialized, Deserialized, LevelDeserialized, MapDeserialized, ReinforcedSpawnerDeserialized, SpawnerEnemyDeserialized, TowerDeserialized}
import backend.mapHandling.Coordinates
import filehandling.FileHandler

import scala.collection.mutable


/**
 * Testataan, että FileHandler tallentaa ja lataa tasot sekä kartat.
 */
class TestGameSavingSpec extends AnyWordSpec with Matchers {
  val workingDir = os.pwd / "src" / "main" / "scala" / "filehandling" / "testData"
  val filehandler = FileHandler(workingDir)

  val baseEnemy = BasicEnemyDeserialized("1", 5, 5, 5)
  val spawnerEnemy = SpawnerEnemyDeserialized("2", 5, 5, ArrayBuffer("1", "1", "1"), 5)
  val reinforcedSpawnerEnemy = ReinforcedSpawnerDeserialized("3", 5, 5, 5, ArrayBuffer("1", "1", "1"), 5)

  val enemies = ArrayBuffer[Deserialized](baseEnemy, spawnerEnemy, reinforcedSpawnerEnemy)

  enemies.foreach(filehandler.addMapping(_)) // lisätään viholliset

  val basictower = BasicTowerDeserialized("1", "1", 1, 10, 200)
  val towers = ArrayBuffer[TowerDeserialized](basictower)

  towers.foreach(filehandler.addMapping(_)) // lisätään tornit

  // luodaan taso, joka tallennetaan
  val level = LevelDeserialized(
    id = "1",
    enemies = Map("1" -> 2, "2" -> 3, "3" -> 2),
    camouflaged = false
  )

  val levels = ArrayBuffer(level)

  levels.foreach(filehandler.addMapping(_)) // lisätään kartta

  // luodaan kartta, joka tallennetaan
  val y = ArrayBuffer[Double](0, 20, 10, 30, 49, 50, 5)
  val x = ArrayBuffer[Double](0, 20, 40, 60, 80, 100, 120)

  val map = MapDeserialized(
    id = "1",
    roadPoints = x.zip(y).map( coords => Coordinates(coords._1, coords._2) )
  )

  val maps = ArrayBuffer(map)
  maps.foreach(filehandler.addMapping(_))


  "The filehandler" should {

    "contain every enemy in the correct indexes" in {

      val mappings = filehandler.enemyIdMapping

      mappings should contain allElementsOf{enemies.map(enemy => enemy.id -> enemy)}
    }

    "should correctly save and load enemies into file" in {
      filehandler.saveEnemies()
      val loadedEnemies = filehandler.loadEnemies

      loadedEnemies should contain allElementsOf{enemies}
    }

    "should correctly save the level" in {
      filehandler.saveLevels()
      filehandler.saveMaps()

      filehandler.updateMappings()
      val loadedLevels = filehandler.levels

      loadedLevels should contain allElementsOf{levels}
    }

    "should correctly save the game" in {
      filehandler.saveMaps()

      filehandler.updateMappings()
      val loadedMaps = filehandler.maps

      loadedMaps should contain allElementsOf {maps}
    }
  }
}