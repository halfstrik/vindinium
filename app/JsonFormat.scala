package org.vindinium.server

import controllers.routes
import play.api.libs.json._
import system.Replay

object JsonFormat {

  def apply(i: system.PlayerInput, host: String): JsObject = Json.obj(
    "game" -> apply(i.game, false),
    "hero" -> i.hero.map(apply(_, i.game)),
    "token" -> i.token,
    "viewUrl" -> ("http://" + host + routes.Game.show(i.game.id).url),
    "playUrl" -> ("http://" + host + routes.Api.move(i.game.id, i.token).url)
  ).noNull

  def apply(g: Game, compressed: Boolean): JsObject = Json.obj(
    "id" -> g.id,
    "turn" -> g.turn,
    "maxTurns" -> g.maxTurns,
    "heroes" -> JsArray(g.heroes map { apply(_, g) }),
    "board" -> Json.obj(
      "size" -> g.board.size,
      "tiles" -> Compressor((g.board.posTiles map {
        case (pos, tile) => (g hero pos).fold(tile.render)(_.render)
      }).mkString, compressed)
    ),
    "finished" -> g.finished
  ).noNull

  def apply(h: Hero, g: Game): JsObject = Json.obj(
    "id" -> h.id,
    "name" -> h.name,
    "userId" -> h.userId,
    "elo" -> h.elo,
    "pos" -> apply(h.pos),
    "life" -> h.life,
    "gold" -> h.gold,
    "mineCount" -> g.board.countMines(h.id),
    "spawnPos" -> apply(g.spawnPosOf(h)),
    "crashed" -> h.crashed
  ).noNull

  def apply(p: Pos) = Json.obj("x" -> p.x, "y" -> p.y)

  private object Compressor {

    private val Spaces = """(\s+)""".r
    private val Hashes = """(#+)""".r

    def apply(str: String, enable: Boolean) =
      if (enable) str
      else Spaces.replaceAllIn(
        Hashes.replaceAllIn(str, m => s"${m.group(0).size / 2}#"),
        m => s"${m.group(0).size / 2} ")
  }

  private implicit final class PimpedJsObject(js: JsObject) {
    def noNull = JsObject {
      js.fields collect {
        case (key, value) if value != JsNull ⇒ key -> value
      }
    }
  }
}
