package com.biosimilarity.emeris


import JsonHelper._
import AgentDataSet._

object SocketProtocol {

  def jsonTypes: List[Class[_]] = List(classOf[CreateNodes], classOf[Message], classOf[Message])

  val serverUid = Uid()
  
  sealed trait MessageBody extends JsonCapable {
    lazy val createMessage = Message(Uid(), serverUid, this)
  }
  
  case class Message(uid: Uid, sender: Uid, body: MessageBody) extends JsonCapable

  case class CreateNodes(nodes: List[Node], labelRoots: List[Uid] = Nil) extends MessageBody
  
  case class RemoveNodes(uidList: List[Uid]) extends MessageBody
  
  
  def parseJson(jsonString: String) = {
	  import net.liftweb.json._
	  import net.liftweb.json.JsonDSL._
	  parse(jsonString).extract[Message]
  }
  
}
