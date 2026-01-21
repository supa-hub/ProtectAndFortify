import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import scala.collection.mutable.ArrayBuffer

import backend.jsonModels.{Deserialized, BasicEnemyDeserialized, SpawnerEnemyDeserialized, ReinforcedSpawnerDeserialized}
import filehandling.FileHandler

class TestenemiesSpec extends AnyWordSpec with Matchers {

  val baseEnemy = BasicEnemyDeserialized("1", 5, 5, 5)
  val spawnerEnemy = SpawnerEnemyDeserialized("2", 5, 5, ArrayBuffer("1", "1", "1"), 5)
  val reinforcedSpawnerEnemy = ReinforcedSpawnerDeserialized("3", 5, 5, 5, ArrayBuffer("1", "1", "1"), 5)

  val enemies = ArrayBuffer[Deserialized](baseEnemy, spawnerEnemy, reinforcedSpawnerEnemy)

  val workingDir = os.pwd / "src" / "main" / "scala" / "filehandling" / "testData"
  val filehandler = FileHandler(workingDir)

  enemies.foreach(filehandler.addMapping(_))


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
  }
}