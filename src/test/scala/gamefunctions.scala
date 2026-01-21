import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.*

import scala.collection.mutable.ArrayBuffer
import backend.jsonModels.{BasicEnemyDeserialized, Deserialized, ReinforcedSpawnerDeserialized, SpawnerEnemyDeserialized, TowerDeserialized}
import backend.mapHandling.Coordinates
import backend.tower.BasicTower
import backend.Game
import filehandling.FileHandler


/**
 * Testataan Game -luokan toimivuuksia
 */
class TestGameSpec extends AnyWordSpec with Matchers {
  val workingDir = os.pwd / "src" / "main" / "scala" / "filehandling" / "testData"
  val fileHandler = FileHandler(workingDir)

  fileHandler.updateMappings() // päivitetään kaikki mallit tallennettujen tiedostojen avulla fileHandler olioon

  val startDelay = 10
  val game = Game("1", fileHandler, levelStartDelay = startDelay) // luodaan peli


  // tarkistetaan, että lisättyjen vihollisten ja tornien määrä on oikein,
  // sekä että lisätyn tornin tiedot ovat oikein
  "The Game class" should {

    "contain 1 enemy after delayBetweenSpawns goes to 0 and 1 tick" in {
      for _ <- 0 to startDelay + 30 do game.tick()
      game.getState.enemies.length shouldBe 1
    }

    "Created enemies location shouldn't not equal the starting pos after 1 tick" in {
      game.tick()
      game.getState.enemies.head.getLocation should not equal game.currMap.startingPoint
    }

    "there should be no towers at the start" in {
      game.getState.towers.length shouldBe 0
    }

    "correctly add a tower when a valid id is given" in {
      game.placeTower("soldier", Coordinates(0, 0))
      game.getState.towers.length shouldBe 1
    }


    "The added tower should" should {
      "contain the same game" in {
        game.getState.towers.head match
          case a: BasicTower => a.game shouldBe game
          case _ => throw Exception("stored tower was not of the correct type")
      }

      "contain the correct coordinates" in {
        game.getState.towers.head match
          case a: BasicTower => a.location shouldBe Coordinates(0, 0)
          case _ => throw Exception("stored tower was not of the correct type")
      }
    }

  }
}