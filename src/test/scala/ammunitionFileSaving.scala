import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable.ArrayBuffer
import backend.jsonModels.{AmmunitionDeserialized, BulletDeserialized, Deserialized, ExplosiveBulletDeserialized, TowerDeserialized}
import filehandling.FileHandler

class TestAmmoSpec extends AnyWordSpec with Matchers {

  val basicBullet = BulletDeserialized("basic_ammunition", 2, 1, 10)
  val basicBullet1 = BulletDeserialized("rocket", 3, 2, 10)

  val ammunition = ArrayBuffer[AmmunitionDeserialized](basicBullet, basicBullet1)

  val workingDir = os.pwd / "src" / "main" / "scala" / "filehandling" / "testData"
  val filehandler = FileHandler(workingDir)

  ammunition.foreach(filehandler.addMapping(_))


  "The filehandler" should {

    "contain every ammo in the correct indexes" in {

      val mappings = filehandler.ammunitionIdMapping

      mappings should contain allElementsOf{ammunition.map(enemy => enemy.id -> enemy)}
    }

    "should correctly save and load towers into file" in {
      filehandler.saveAmmunition()
      val loadedEnemies = filehandler.loadAmmunition

      loadedEnemies should contain allElementsOf{ammunition}

    }
  }
}