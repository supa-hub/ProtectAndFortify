import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable.ArrayBuffer
import backend.jsonModels.{BasicTowerDeserialized, BoostingTowerDeserialized, BulletDeserialized, Deserialized, ExplosiveBulletDeserialized, TowerDeserialized}
import backend.tower.{Boost, DmgMultiplier, FireRate, RangeBoost, CamoVision}
import filehandling.FileHandler

import scala.collection.mutable

class TestTowersSpec extends AnyWordSpec with Matchers {

  val possibleBoosts: mutable.Map[Boost, Int] = mutable.Map(
    DmgMultiplier(multiplier = 1.5) -> 20,
    FireRate(multiplier = 1.2) -> 30,
    RangeBoost(multiplier = 1.5) -> 50,
    CamoVision() -> 30
  )

  val basictower = BasicTowerDeserialized("soldier", "basic_ammunition", 10, 10, 200)
  val basictower1 = BasicTowerDeserialized("helicopter", "rocket", 150, 10, 300)
  val boostingtower = BoostingTowerDeserialized("tent", 50, possibleBoosts, 200)

  val towers = ArrayBuffer[TowerDeserialized](basictower, basictower1, boostingtower)

  val workingDir = os.pwd / "src" / "main" / "scala" / "filehandling" / "testData"
  val filehandler = FileHandler(workingDir)

  towers.foreach(filehandler.addMapping(_))


  "The filehandler" should {

    "contain every tower in the correct indexes" in {

      val mappings = filehandler.towerIdMapping

      mappings should contain allElementsOf{towers.map(tower => tower.id -> tower)}
    }

    "should correctly save and load towers into file" in {
      filehandler.saveTowers()
      val loadedEnemies = filehandler.loadTowers

      loadedEnemies should contain allElementsOf{towers}

    }
  }
}