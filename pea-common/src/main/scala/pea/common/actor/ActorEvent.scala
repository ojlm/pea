package pea.common.actor

import pea.common.model.{ApiCode, ApiMsg}
import pea.common.util.StringUtils

case class ActorEvent(
                       `type`: String = StringUtils.EMPTY,
                       code: String = ApiCode.OK,
                       msg: String = ApiMsg.SUCCESS,
                       data: Any = null)

object ActorEvent {
  val TYPE_INIT = "init"
  val TYPE_LIST = "list"
  val TYPE_ITEM = "item"
  val TYPE_OVER = "over"
  val TYPE_NOTIFY = "notify"
  val TYPE_ERROR = "error"
}

object InitActorEvent {
  def apply(): ActorEvent = new ActorEvent(`type` = ActorEvent.TYPE_INIT)
}

object OverActorEvent {
  def apply(data: Any): ActorEvent = new ActorEvent(`type` = ActorEvent.TYPE_OVER, data = data)
}

object ListActorEvent {
  def apply(data: Any): ActorEvent = new ActorEvent(`type` = ActorEvent.TYPE_LIST, data = data)
}

object ItemActorEvent {
  def apply(data: Any): ActorEvent = new ActorEvent(`type` = ActorEvent.TYPE_ITEM, data = data)
}

object NotifyActorEvent {
  def apply(msg: String): ActorEvent = new ActorEvent(`type` = ActorEvent.TYPE_NOTIFY, msg = msg)
}

object ErrorActorEvent {
  def apply(msg: String): ActorEvent = new ActorEvent(`type` = ActorEvent.TYPE_ERROR, code = ApiCode.ERROR, msg = msg)
}
