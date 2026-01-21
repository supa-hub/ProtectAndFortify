package backend.tower

import upickle.default.ReadWriter

sealed trait Boost derives ReadWriter:
  def id: String
end Boost

case class DmgMultiplier(id: String = "dmgMultiplier", multiplier: Double) extends Boost
case class FireRate(id: String = "fireRate", multiplier: Double) extends Boost
case class RangeBoost(id: String = "rangeBoost", multiplier: Double) extends Boost
case class CamoVision(id: String = "camovision", canSee: Boolean = true) extends Boost