package backend

import backend.ammunition.{Ammunition, BulletFactory, ExplosiveBullet, FollowingBullet}
import backend.enemy.{DeathVariants, Enemy, EnemyFactory}
import backend.tower.{Boost, BoostingTower, ShootingTower, Tower, TowerFactory}
import backend.jsonModels.TowerDeserialized
import backend.mapHandling.{Coordinates, RouteHandler}
import fs2.Stream
import cats.data.EitherT
import extensions.toEitherT
import cats.*

import scala.collection.mutable.{ArrayBuffer, Map, Queue}
import filehandling.FileHandler


/**
 * Kuvaa yksittäisen pelisession loogista puolta backendissä
 * @param mapId kartan id-jota pelataan
 * @param fileHandler olio, joka lukee ja kirjoittaa json-tiedostot
 * @param width kartan leveys
 * @param height kartan korkeus
 * @param playerHP pelaajan hp
 * @param playersMoney pelaajan raha
 * @param levelStartDelay viive uuden tason aloituksen ja vihollisten tulemisen välillä. Eli kun uusi taso alkaa, tämän verran täytyy odottaa, että viholliset tulevat
 */
class Game(
  private val mapId: String,
  private val fileHandler: FileHandler = FileHandler(),
  private val width: Int = 500,
  private val height: Int = 500,
  private var playerHP: Int = 100,
  private var playersMoney: Int = 100,
  private var levelStartDelay: Int = 200
):
  fileHandler.updateMappings() // päivitetään kaikki mallit tallennettujen tiedostojen avulla filehandler olioon

  private var enemies: List[Enemy] = Nil
  var towers: List[Tower] = Nil
  var ammunition: List[Ammunition] = Nil
  var currMap: RouteHandler = fileHandler.buildMap(mapId)
  val newEnemies = Queue[Enemy]()

  // näillä luodaan uusia vihollisia
  val enemyFactory = EnemyFactory(fileHandler.enemyIdMapping)
  val towerFactory = TowerFactory(fileHandler.towerIdMapping)
  val bulletFactory = BulletFactory(fileHandler.ammunitionIdMapping)


  private val allLevels = fileHandler.levels.iterator
  private var currLevel = allLevels.next()
  private var levelCount = 1
  private var newLevel = true
  private var finished = false

  private var delayBetweenSpawns = 30


  def getFileHandler = fileHandler
  def isFinished = finished || (playerHP <= 0)
  def playerWon = finished && (playerHP > 0)

  /**
   * siirtyy seuraavaan tasoon, jos se on olemassa
   */
  def nextLevel(): Unit =
    if allLevels.hasNext then
      currLevel = allLevels.next()
    else
      finished = true

  /**
   * tallentaa pelin, jotta sitä voi jatkaa myöhemmin
   */
  def saveGame(): Unit =
    fileHandler.saveGame(
      towers,
      enemies,
      ammunition,
      currMap.id,
      width,
      height,
      playerHP,
      playersMoney,
      levelStartDelay,
      levelCount
    )


  /**
   * vastaa backendissä yhtä pelissä kulunutta framea.
   * Täytyy kutsua jatkuvasti pelin aikana
   */
  def tick() =
    if !isFinished then
      ammunition = towers.flatMap:
        case a: ShootingTower => a.shoot
        case _ => None
      ::: ammunition

      towers.foreach {
        case a: BoostingTower => a.giveBoosts()
        case _ =>
      }

      ammunition = ammunition.filter(anAmmo =>
        anAmmo.location.x > 0
          && anAmmo.location.y > 0
          && anAmmo.location.x < width
          && anAmmo.location.y < height
          && anAmmo.getPenetrationRate > 0
      )

      ammunition.foreach(_.move())

      /*
      enemies = enemies
        .tapEach( anEnemy =>
          ammunition.foreach( anAmmo =>
            if (anEnemy.getLocation - anAmmo.location).length < 30 then
              anAmmo match
                case explosive: ExplosiveBullet =>
                  val auxRange = Range(explosive.explosionRadius, explosive.location)
                  enemies.foreach( enemy =>
                    if auxRange.inside(enemy.getLocation) then
                      enemy.takeHit(explosive)
                      explosive.penetrationRate = 0
                  )


                case _ =>
                  if anAmmo.penetrationRate > 0 then
                    anEnemy.takeHit(anAmmo)
          )
        )
        .tapEach(_.move())
        .tapEach(anEnemy =>
          if anEnemy.isDead then
            anEnemy.death() match
              case DeathVariants.BaseDeath(value) => playersMoney += value

              case DeathVariants.SpawnerDeath(value, spawnedEnemies) =>
                playersMoney += value
                newEnemies ++= spawnedEnemies

              case DeathVariants.OutOfBoundsDeath(remainingHp) =>
                playerHP -= remainingHp

              case _ =>
        )
        .filter(anEnemy => !anEnemy.isDead) // poistetaan kuolleet viholliset
      */

      enemies = Stream
        .emits(enemies)
        .covary[Id]
        .evalTap(_.move())
        .evalTap(anEnemy =>
          ammunition.foreach(anAmmo =>
            if (anEnemy.getLocation - anAmmo.location).length < 30 then
              anAmmo match
                case explosive: ExplosiveBullet =>
                  val auxRange = Range(explosive.explosionRadius, explosive.location)
                  enemies.foreach( enemy =>
                    if auxRange.inside(enemy.getLocation) then
                      enemy.takeHit(explosive)
                      explosive.penetrationRate = 0
                  )

                case _ =>
                  if anAmmo.penetrationRate > 0 then
                    anEnemy.takeHit(anAmmo)
          )
        )
        .evalTapChunk(enemy =>
          if enemy.isDead then
            enemy.death() match
              case DeathVariants.BaseDeath(value) =>
                playersMoney += value

              case DeathVariants.SpawnerDeath(value, spawnedEnemies) =>
                playersMoney += value
                newEnemies ++= spawnedEnemies

              case DeathVariants.OutOfBoundsDeath(remainingHp) =>
                playerHP -= remainingHp

              case _ =>
        )
        .filter(anEnemy => !anEnemy.isDead)
        .compile
        .toList



      if newEnemies.nonEmpty then
        enemies = newEnemies.dequeue() :: enemies // lisätään uudet viholliset

      if levelStartDelay > 0 then
        levelStartDelay -= 1

      else
        if delayBetweenSpawns <= 0 then
          // lisätään uusi vihollinen kun uusi taso alkoi
          getEnemy(currLevel.enemies, currMap.startingPoint.copy(), currLevel.camouflaged) match
            case Some(anEnemy) => enemies = anEnemy :: enemies
            case None =>

          delayBetweenSpawns = 30

        else
          delayBetweenSpawns -= 1


      if levelStartDelay == 0 && enemies.isEmpty && currLevel.enemies.values.sum == 0 then
        levelStartDelay = 360
        levelCount += 1
        nextLevel()

  /**
   * saat tämänhetkisen pelin tilan
   * @return
   */
  def getState: GameState = GameState(
      enemies = enemies,
      towers = towers,
      ammunition = ammunition,
      map = currMap,
      playerHP = playerHP,
      playerMoney = playersMoney,
      level = levelCount,
      gameFinished = isFinished,
      playerWon = playerWon
    )


  /**
   * metodi tarkistamaan, että annettu torni voidaan sijoittaa annetulle paikalle
   * @param tower tornin json malli
   * @param location sijainti, johon halutaan sijoittaa annettu torni
   * @return <true>, jos voidaan sijoittaa annetulle sijainnille, <false> jos ei voida sijoittaa
   */
  def canPlace(tower: TowerDeserialized, location: Coordinates): Boolean =
    tower.value <= playersMoney && currMap.canPlace(Range(10, location))

  /**
   * Myy tornin ja saa 80% sen arvosta
   * @param tower torni, joka halutaan myydä
   * @return jos torni voidiin,m yydä, niin <true>, muulloin <false>
   */
  def sellTower(tower: Tower): Boolean =
    if towers.contains(tower) then
      towers = towers.filter(_ !=  tower)
      playersMoney += (tower.value * 0.8).toInt
      true
    else
      false



  /**
   * myy sen tornin, jonka .toString metodin paluuarvo vastaa annettua id:tä
   * @param id jonkin tornin .toString -metdoni antama arvo
   * @return jos torni myytiin onnistuneesti, niin <true>, muulloin <false>
   */
  def sellTowerByIdInstance(id: String): Boolean =
    towers.find(_.toString == id) match
      case Some(tower) =>
        towers = towers.filter(_ != tower)
        playersMoney += (tower.value * 0.8).toInt
        true

      case None => false

  /**
   * Myy tornin ja saa 80% sen arvosta
   *
   * @param id tornin id, joka halutaan myydä, jos on useampi samantyyppinen torni, niin myy ensimmäisen sijoitetun, jolla tämä id
   * @return jos torni voidiin,m yydä, niin <true>, muulloin <false>
   */
  def sellTowerById(id: String): Boolean =
    towers.find(_.id == id) match
      case Some(tower) =>
        towers = towers.filter(_ != tower)
        playersMoney += (tower.value * 0.8).toInt
        true

      case None => false


  /**
   * Sijoittaa tornin pelikartalle
   * @param towerId tornin id
   * @param location tornin sijainti
   */
  def placeTower(towerId: String, location: Coordinates): Unit =
    fileHandler.towerIdMapping.get(towerId) match
        case Some(towerDeserialized) =>
          if canPlace(towerDeserialized, location) then
            towerFactory.createTowerById(towerId, location, this) match
              case Some(aTower) =>
                towers = aTower :: towers
                playersMoney -= towerDeserialized.value

              case None =>

        case None =>


  /**
   * Osta boosti, jonka BoostingTower antaa sen lähellä oleville torneille
   * @param tower torni, jolla hankitaan boosti
   * @param boost boosti, joka halutaan ostaa
   */
  def buyBoost(tower: BoostingTower, boost: Boost): Unit =
    tower.getBoosts.get(boost) match
      case Some(price) =>
        if price <= playersMoney then
          tower.buyBoost(boost)
          playersMoney -= price

      case None =>


  /**
   *
   * Lisää peliin yksi vihollinen siitä avain-arvo parista, jossa
   * enemyCounts:in arvo ei vielä ole 0
   * @param startPos aloitussijainti, josta vihollinen lähtee liikkeelle
   * @param camouflaged onko vihollinen maastoutunut, eli tornit, joilla ei ole maastoutusnäköä, eivät nää vihollista
  */
  private def getEnemy(enemyCounts: Map[String, Int], startPos: Coordinates, camouflaged: Boolean): Option[Enemy] =
    enemyCounts.find(anEnemy => anEnemy._2 > 0) match
      case Some(enemyTuple) =>
        enemyFactory.createEnemyById(enemyTuple._1, startPos, camouflaged)(using currMap.copy) match
          case Some(anEnemy) =>
            enemyCounts(enemyTuple._1) -= 1
            Some(anEnemy)

          case None => None
      case None => None

end Game




object Game:
  /**
   * Aloittaa tallennetun pelin
   * @return Game-olio, joka sisältää tallennetun pelin tilan
   */
  def startSavedGame(): Game =
    val fileHandler = FileHandler()

    val savedGame = fileHandler.loadSavedGame

    val game = Game(savedGame.mapId, fileHandler, savedGame.width, savedGame.height, savedGame.playerHP, savedGame.playerMoney, savedGame.levelStartDelay)
    game.enemies = Nil
    game.towers = Nil
    game.ammunition = Nil

    game.enemies ++= savedGame.enemies.flatMap(game.enemyFactory.createEnemyBySerializer(_)(using game.currMap.copyFull))
    game.towers ++= savedGame.towers.flatMap(
      game.towerFactory.createTowerBySerializer(_, game)
    )

    game.ammunition ++= savedGame.ammunition.flatMap(game.bulletFactory.createBulletBySerializer(_))

    if game.enemies.nonEmpty then
      game.ammunition.foreach {
        case a: FollowingBullet => a.setTarget(game.enemies.head)
        case _ =>
      }

    var i = 0
    while i < savedGame.levelIdx - 1 do
      game.nextLevel()
      i += 1

    game.levelCount = savedGame.levelIdx

    game

end Game