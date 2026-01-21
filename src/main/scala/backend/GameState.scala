package backend

import backend.ammunition.Ammunition
import backend.enemy.Enemy
import backend.mapHandling.RouteHandler
import backend.tower.Tower

import scala.collection.mutable.ArrayBuffer


case class GameState(
  enemies: List[Enemy],
  towers: List[Tower],
  ammunition: List[Ammunition],
  map: RouteHandler,
  playerHP: Int,
  playerMoney: Int,
  level: Int,
  gameFinished: Boolean,
  playerWon: Boolean
)
