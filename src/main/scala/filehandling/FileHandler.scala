package filehandling

import backend.ammunition.Ammunition
import backend.enemy.Enemy
import backend.jsonModels.{AmmunitionDeserialized, Deserialized, GameSerializer, LevelDeserialized, MapDeserialized, Serializer, TowerDeserialized}
import upickle.default.{read, write}
import backend.mapHandling.RouteHandler
import backend.tower.Tower
import os.Path

import scala.collection.mutable.{ArrayBuffer, Map}
import scala.collection.concurrent.TrieMap


/**
 * Luokka, joka lukee ja kirjoittaa tiedostoihin.
 */
class FileHandler(private val workingDir: Path = os.pwd / "src" / "main" / "scala" / "filehandling"):
  

  /**
   * Varmistetaan, että pelin eri säikeet eivät muokkaa samaa tietoa samaan aikaan käyttämällä
   * TrieMap kokoelmaa
   */
  val enemyIdMapping: TrieMap[String, Deserialized] = TrieMap()

  val towerIdMapping: TrieMap[String, TowerDeserialized] = TrieMap()

  val ammunitionIdMapping: TrieMap[String, AmmunitionDeserialized] = TrieMap()

  val levels: ArrayBuffer[LevelDeserialized] = ArrayBuffer()
  val maps: ArrayBuffer[MapDeserialized] = ArrayBuffer()
  
  
  /**
   * Seuraavia metodeja käytetään lataamaan tiedot eri tiedostoista, kuten tallennetusta tasosta,
   * tai perusteidot hahmoista
   */
  def loadTowers: ArrayBuffer[TowerDeserialized] = read(os.read(workingDir / "datafiles" / "towerData.json"))
  def loadEnemies: ArrayBuffer[Deserialized] = read(os.read(workingDir / "datafiles" / "enemyData.json"))
  def loadAmmunition: ArrayBuffer[AmmunitionDeserialized] = read(os.read(workingDir / "datafiles" / "ammunitionData.json"))
  def loadLevels: ArrayBuffer[LevelDeserialized] = read(os.read(workingDir / "datafiles" / "levelData.json"))
  def loadMaps: ArrayBuffer[MapDeserialized] = read(os.read(workingDir / "datafiles" / "mapData.json"))

  def loadSavedGame: GameSerializer = read(os.read(workingDir / "datafiles" / "savedGame.json"))

  def saveEnemies(): Unit = os.write.over(workingDir / "datafiles" / "enemyData.json", write(enemyIdMapping.values, 5))
  def saveTowers(): Unit = os.write.over(workingDir / "datafiles" / "towerData.json", write(towerIdMapping.values, 5))
  def saveAmmunition(): Unit = os.write.over(workingDir / "datafiles" / "ammunitionData.json", write(ammunitionIdMapping.values, 5))
  def saveLevels(): Unit = os.write.over(workingDir / "datafiles" / "levelData.json", write(levels, 5))
  def saveMaps(): Unit = os.write.over(workingDir / "datafiles" / "mapData.json", write(maps, 5))

  /**
   * Seuraavia metodeja käytetään tallentamaan tiedot tiedostoihin.
   */
  def saveGame(
    towerData: List[Tower],
    enemyData: List[Enemy],
    ammo: List[Ammunition],
    mapId: String,
    width: Int,
    height: Int,
    playerHP: Int,
    playerMoney: Int,
    levelStartDelay: Int,
    levelIdx: Int
  ): Unit =
    val state = GameSerializer(
      enemies = enemyData.map(_.toJson),
      towers = towerData.map(_.toJson),
      ammunition = ammo.map(_.toJson),
      width = width,
      height = height,
      mapId = mapId,
      playerHP = playerHP,
      playerMoney = playerMoney,
      levelStartDelay = levelStartDelay,
      levelIdx = levelIdx
    )

    os.write.over(workingDir / "datafiles" / "savedGame.json", write(state, 5))



  /**
   * Luodaan pelin kartta annetun id:n perusteella.
   * @param mapId Määrittää mitä karttaa pelataan
   * @return
   */
  def buildMap(mapId: String): RouteHandler =
    loadMaps.find(_.id == mapId) match
      case Some(successful) => RouteHandler(5, successful)
      case None => throw Exception(s"Could not find map with the id: $mapId")


  def updateMappings(): Unit =
    /**
     * TrieMappien tietojen päivitys on tehty niin, että
     * ei käytetä .clear() metodia, jotta ei synny tilannetta, että
     * joku etsii olemassa olevaa tietoa, mutta sitä ei ole vielä lisätty
     * kokoelmaan.
     */
    enemyIdMapping.clear()
    enemyIdMapping ++= loadEnemies.map(anEnemy => (anEnemy.id, anEnemy))

    towerIdMapping.clear()
    towerIdMapping ++= loadTowers.map(aTower => (aTower.id, aTower))

    ammunitionIdMapping.clear()
    ammunitionIdMapping ++= loadAmmunition.map(anAmmo => (anAmmo.id, anAmmo))

    levels.clear()
    maps.clear()

    levels ++= loadLevels
    maps ++= loadMaps


  /**
   * Lisätään viittaus johonkin viholliseen tai torniin.
   * Käytetään useimmiten testauksessa.
   * @param updatable yksi mahdollisista Deserialized -tyypeistä, jotka tallennetaan kokoelmiin
   */
  def addMapping(updatable: Deserialized | TowerDeserialized | LevelDeserialized | MapDeserialized): Unit =
    updatable match
      case a: TowerDeserialized => towerIdMapping.addOne((a.id, a))
      case b: AmmunitionDeserialized => ammunitionIdMapping.addOne((b.id, b))
      case c: LevelDeserialized => levels += c
      case d: MapDeserialized   => maps += d
      case e: Deserialized => enemyIdMapping.addOne((e.id, e))

  def getMap(mapId: String) = loadMaps.filter(_.id == mapId).head
  
end FileHandler






