// -*- mode: Scala;-*- 
// Filename:    WebSocket.scala 
// Authors:     lgm                                                    
// Creation:    Fri Dec  2 14:28:24 2011 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.lib.websocket

import com.biosimilarity.lift.lib.moniker._

import net.liftweb.amqp._

import scala.collection.mutable.Queue
import scala.collection.mutable.HashMap
import scala.collection.mutable.MapProxy
import scala.collection.SeqProxy
import scala.util.continuations._

import scala.concurrent.{Channel => Chan, _}
import scala.concurrent.cpsops._

import _root_.com.rabbitmq.client.{ Channel => RabbitChan, _}
import _root_.scala.actors.Actor

import org.eclipse.jetty.websocket.WebSocket

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver

import org.apache.http.HttpResponse
import org.apache.http.HttpEntity
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.nio.conn.ClientConnectionManager
//import org.apache.http.nio.client.methods.BaseHttpAsyncRequestProducer
import org.apache.http.nio.client.HttpAsyncClient
import org.apache.http.nio.concurrent.FutureCallback

import java.io.IOException;
import java.util.concurrent.Future;

import java.net.URI
import _root_.java.io.ObjectInputStream
import _root_.java.io.ByteArrayInputStream
import _root_.java.util.Timer
import _root_.java.util.TimerTask

case class WSMgr( 
  socketMap : HashMap[WebSocket,WebSocket.Connection]  
) extends MapProxy[WebSocket,WebSocket.Connection] {
  override def self = socketMap
}

case object theWSMgr extends WSMgr(
  new HashMap[WebSocket,WebSocket.Connection]( )
)

case class QueuingWebSocket( 
  wsMgr : WSMgr,
  queue : Queue[String]
) extends WebSocket 
     with WebSocket.OnTextMessage
     with SeqProxy[String]
{
  override def self = queue

  override def onOpen(
    connection: WebSocket.Connection
  ) : Unit = {
    // BUGBUG -- lgm : is this thread safe?
    println( "in onOpen with " + connection )
    wsMgr += ( ( this, connection ) )
  }
  
  override def onClose(
    closeCode: Int,
    message: String
  ) : Unit = {
    // BUGBUG -- lgm : is this thread safe?
    println( "in onClose with " + closeCode + " and " + message )
    wsMgr -= this
  }
  
  override def onMessage(
    message: String
  ) : Unit = {
    // BUGBUG -- lgm : is this thread safe?
    println( "in onMessage with " + message )
    queue += message
  }    
}


package usage {
/* ------------------------------------------------------------------
 * Mostly self-contained object to support unit testing
 * ------------------------------------------------------------------ */ 

  import com.biosimilarity.lift.model.store._
  import com.biosimilarity.lift.model.store.usage._
  import com.biosimilarity.lift.model.store.usage.PersistedMonadicTS._

  import com.biosimilarity.magritte.json._
  import com.biosimilarity.magritte.json.Absyn._
  import java.io.StringReader

  trait Argonaut {
    def lexer (str : String) = new Yylex(new StringReader(str))
    def parser (str : String) = new parser(lexer(str))
    def parseJSONStr( str : String ) =
      {
	try {
	  Some((parser(str)).pJSONObject())
	}
	catch {
	  case _ => None
	}
      }
  }   

}
