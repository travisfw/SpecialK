// -*- mode: Scala;-*- 
// Filename:    SocketServlet.scala<2> 
// Authors:     lgm                                                    
// Creation:    Fri Dec  2 14:47:34 2011 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.lib.websocket

import scala.collection.mutable.Queue

import javax.servlet.http.HttpServletRequest

import java.io.IOException
import java.util.Set
import java.util.concurrent.CopyOnWriteArraySet
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.websocket.WebSocketServlet
import org.eclipse.jetty.websocket.WebSocket

import net.liftweb.json._
import net.liftweb.json.JsonDSL._

object KvdbHelper {
  
  import com.biosimilarity.lift.model.store._
  import com.biosimilarity.lift.model.store.usage._
  import com.biosimilarity.lift.model.store.xml._
  import com.biosimilarity.lift.model.store.xml.datasets._
  import com.biosimilarity.lift.lib._
  import scala.util.continuations._ 
  import java.net.URI
  import java.util.UUID
  import CCLDSL._
  import com.biosimilarity.lift.lib.websocket.usage.WebsocketMonadicTS
  import com.biosimilarity.lift.lib.websocket.usage.WebsocketMonadicTS._
  //import PersistedMonadicTS._
  import com.biosimilarity.lift.lib.SpecialKURIDefaults._
  import scala.concurrent.ops.spawn

  implicit def toPattern(
    s : String
  ) : CnxnCtxtLabel[String,String,String] with Factual =
    CXQ.fromCaseClassInstanceString(
      s
    ).getOrElse(
      null
    ).asInstanceOf[CnxnCtxtLabel[String,String,String] with Factual]

  implicit def toValue( s : String ) : mTT.Resource = mTT.Ground( s )

}


object QuerySocketServlet {
	
	import KvdbHelper._
	import scala.util.continuations._
	
	val kvdb = usage.WebsocketMonadicTS.loopBack("queryByExample")		
	
	def rStrOpt(key: String)(implicit jv: JValue): Option[String] = {
	  (jv \ key) match {
	    case JString(s) => Some(s)
      case JNothing => None	
	    case _ => sys.error("invalid value for " + key + " in " + jv)	  
	  }
	}

	def rStr(key: String)(implicit json: JValue): String = {
	  rStrOpt(key).getOrElse(sys.error("missing required field " + key))
  }
	
		
	class QuerySocket extends WebSocket 
	     with WebSocket.OnTextMessage
	{
	  
    var connection: WebSocket.Connection = _

	  override def onOpen(
	    connection0: WebSocket.Connection
	  ) : Unit = {
	    connection = connection0
	  }

	  override def onClose(
	    closeCode: Int,
	    message: String
	  ) : Unit = {
	  }

	  override def onMessage(
	    message: String
	  ) : Unit = {
  		implicit val json = parse(message)
  		val uid = rStr("uid")
  		val verb = rStr("verb").toLowerCase
  		val key = rStr("key")		
  		val value = rStrOpt("value")		

  		verb match {
  			case "get" => doGet(uid, key)
  			case "put" => doPut(uid, key, value.get)
  			case "subscribe" => doSubscribe(uid, key)
  			case "publish" => doPublish(uid, key, value.get)
        // case "read" => doRead(uid, key)
        // case "store" => doStore(uid, key, value.get)
  		}

	  }

  	def sendResponse(uid: String, response: String) = {
  	  val json = ("uid" -> uid) ~ ("response" -> response)
  	  connection.sendMessage(pretty(render(json)))
  	}

    //     def doRead(uid: String, key: String) = {
    //   reset {
    //     for( 
    //       value <- kvdb.read( key ) 
    //     ) {
    //      sendResponse(uid, value.toString)
    //    }       
    //   }
    // }

    def doSubscribe(uid: String, key: String) = {
  	  reset {
  	    for( 
  	      value <- kvdb.subscribe( key ) 
  	    ) {
  		    sendResponse(uid, value.toString)
  		  }	    	
  	  }
    }  	

    //     def doStore(uid: String, key: String, value: String) = {
    //   reset {
    //     kvdb.store( key, value )
    //     sendResponse(uid, "publish complete" )
    //   }
    // }

    def doPut(uid: String, key: String, value: String) = {
  	  reset {
  	    kvdb.put( key, value )
  	    sendResponse(uid, "put complete" )
  	  }
  	}

    def doPublish(uid: String, key: String, value: String) = {
  	  reset {
  	    kvdb.publish( key, value )
  	    sendResponse(uid, "publish complete" )
  	  }
  	}
  	
    def doGet(uid: String, key: String) = {
  	  reset {
  	    for( 
  	      value <- kvdb.get( key ) 
  	    ) {
  		    sendResponse(uid, value.toString)
  		  }	    	
  	  }
  	}
  }

}

import QuerySocketServlet._

class QuerySocketServlet extends org.eclipse.jetty.websocket.WebSocketServlet {
  def doWebSocketConnect(
    request : HttpServletRequest,
    protocol : String
  ) : WebSocket = {    
    new QuerySocket
  }  
}
