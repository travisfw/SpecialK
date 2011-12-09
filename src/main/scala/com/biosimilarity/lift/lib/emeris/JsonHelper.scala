package com.biosimilarity.emeris


import net.liftweb.json._
import net.liftweb.json.JsonDSL._

import AgentDataSet._

object JsonHelper {
  
  trait JsonCapable {
    def asJValue = decompose(this)
  }
    
  val typeHints  = XTypeHints(SocketProtocol.jsonTypes ++ AgentDataSet.jsonTypes)

  implicit val formats = Serialization.formats(typeHints) + UidSerializer

	object UidSerializer extends Serializer[Uid] {
        private val UidClass = classOf[Uid]

        def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Uid] = {
          case (TypeInfo(UidClass, _), json) => json match {
            case JString(value) => Uid(value)
            case x => throw new MappingException("Can't convert " + x + " to Uid")
          }
        }

        def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
          case x: Uid => JString(x.value)
        }
    }
	
	case class XTypeHints(hints: List[Class[_]]) extends TypeHints {
		def hintFor(clazz: Class[_]) = clazz.getName.substring(clazz.getName.lastIndexOf("$")+1)
		def classFor(hint: String) = hints find (hintFor(_) == hint)
	}
	
	def decompose(a: Any) = Extraction.decompose(a)

}