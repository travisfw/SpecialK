package com.biosimilarity.emeris


import java.util.UUID
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet
import scala.collection.mutable.SynchronizedBuffer
import scala.collection.mutable.SynchronizedSet
import java.io.Writer
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
//import net.model3.lang.ClassX
import net.liftweb.json._
import net.liftweb.json.JsonDSL._

import JsonHelper._
import SocketProtocol.CreateNodes

object AgentDataSet extends App {

    def jsonTypes: List[Class[_]] = List(classOf[Address], classOf[Label], classOf[Link], classOf[Person], classOf[Photo])

	trait Node {
	  
		val uid: Uid
		
		def typeName: String = {
		  val classname = getClass.getName
		  classname.substring(classname.indexOf("$")+1)
		}
      	
      	override def hashCode = uid.hashCode
      	
      	override def equals(x: Any) = {
		  x match {
		    case n: Node => n.uid == uid
		    case _ => false
		  }
		}
      	
      	def asJValue = {
		  import JsonHelper._
      	  import net.liftweb.json.Serialization.write
      	  // TODO ugly hack will read the lift json docs to find the right way (tm)
      	  decompose(this)
      	}
      	
	}

	case class Uid(value: String = UUID.randomUUID.toString) {
	  override def toString = value
	}
	
	case class Address(line1: String, line2: String, city: String, state: String, zip: String, uid: Uid = Uid()) extends Node 

	case class Label(name: String, uid: Uid = Uid()) extends Node

	case class Link(from: Uid, to: Uid, uid: Uid = Uid()) extends Node 
	
	case class Person(name: String, uid: Uid = Uid()) extends Node
	
	case class Photo(url: String, uid: Uid = Uid()) extends Node 

	
}


class AgentDataSet {
  
  import AgentDataSet._
	
  var nodes = List[Node]()
  var nodesByUid = Map[Uid,Node]()

  var labelRoots = List[Node]()

  def add[T<:Node](n: T) = synchronized {
    if ( !nodes.contains(n) ) {
	    nodes ::= n
	    nodesByUid += (n.uid -> n)
    }
    n
  }
  
  def remove(uid: Uid) = synchronized {
    nodes = nodes.filter(_ != uid)
	nodesByUid -= uid
  }
  
  def asCreateNodes = CreateNodes(
	nodes
    , labelRoots.map(_.uid)
  )

}


