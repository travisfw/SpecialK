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
  import com.biosimilarity.lift.model.agent._
  import com.biosimilarity.lift.model.msg._

  import com.biosimilarity.lift.lib.UUIDOps
  import com.biosimilarity.lift.lib.moniker._

  import com.biosimilarity.magritte.json._
  import com.biosimilarity.magritte.json.Absyn._

  import scala.xml._
  import scala.collection.MapProxy
  import scala.collection.mutable.Map
  import scala.collection.mutable.HashMap
  import scala.collection.mutable.LinkedHashMap
  import scala.collection.mutable.ListBuffer
  import scala.collection.mutable.MutableList

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

  object WebsocketMonadicTS
     extends PersistedTermStoreScope[String,String,String,String] 
     with UUIDOps
  {
    import com.biosimilarity.lift.lib.SpecialKURIDefaults._
    import com.biosimilarity.lift.model.store.CnxnLeafAndBranch._
    import identityConversions._

    type MTTypes = MonadicTermTypes[String,String,String,String]
    object TheMTT extends MTTypes
    override def protoTermTypes : MTTypes = TheMTT

    type DATypes = DistributedAskTypes
    object TheDAT extends DATypes
    override def protoAskTypes : DATypes = TheDAT
    
    class WebsocketStringMGJ(
      val dfStoreUnitStr : String,
      //override val name : URI,
      override val name : Moniker,
      //override val acquaintances : Seq[URI]
      override val acquaintances : Seq[Moniker]
    ) extends PersistedMonadicGeneratorJunction(
      name, acquaintances
    ) {
      class StringXMLDBManifest(
	override val storeUnitStr : String,
	override val labelToNS : Option[String => String],
	override val textToVar : Option[String => String],
	override val textToTag : Option[String => String]        
      )
      extends XMLDBManifest( database ) {
	override def storeUnitStr[Src,Label,Trgt](
	  cnxn : Cnxn[Src,Label,Trgt]
	) : String = {     
	  cnxn match {
	    case CCnxn( s, l, t ) =>
	      s.toString + l.toString + t.toString
	  }
	}	

	def kvNameSpace : String = "record"

	// BUGBUG -- LGM: Evidence of a problem with this factorization
	override def asCacheValue(
	  ltns : String => String,
	  ttv : String => String,
	  value : Elem
	) : Option[String] = {
	  tweet(
	    "Shouldn't be here!"
	  )
	  None
	}

	override def asStoreValue(
	  rsrc : mTT.Resource
	) : CnxnCtxtLeaf[String,String,String] with Factual = {
	  valueStorageType match {
	    case "CnxnCtxtLabel" => {
	      tweet(
		"warning: CnxnCtxtLabel method is using XStream"
	      )

	      val blob = toXQSafeJSONBlob( rsrc )

	      new CnxnCtxtLeaf[String,String,String](
		Left[String,String](
		  blob
		)
	      )
	    }
	    case "XStream" => {
	      tweet(
		"using XStream method"
	      )

	      val blob = toXQSafeJSONBlob( rsrc )

	      //asXML( rsrc )
	      new CnxnCtxtLeaf[String,String,String](
		Left[String,String]( blob )
	      )
	    }
	    case _ => {
	      throw new Exception( "unexpected value storage type" )
	    }
	  }	  
	}

	def asCacheValue(
	  ccl : CnxnCtxtLabel[String,String,String]
	) : String = {
	  tweet(
	    "converting to cache value"
	  )
	  //asPatternString( ccl )
	  ccl match {
	    case CnxnCtxtBranch(
	      "String",
	      CnxnCtxtLeaf( Left( rv ) ) :: Nil
	    ) => {
	      val unBlob =
		fromXQSafeJSONBlob( rv )

	      unBlob match {
		case rsrc : mTT.Resource => {
		  getGV( rsrc ).getOrElse( "" )
		}
	      }
	    }
	    case _ => {
	      asPatternString( ccl )
	    }
	  }
	}
      
      }

      def persistenceManifest : Option[PersistenceManifest] = {
	val sid = Some( ( s : String ) => s )
	Some(
	  new StringXMLDBManifest( dfStoreUnitStr, sid, sid, sid )
	)
      }
    }
    
    def ptToPt( storeUnitStr : String, a : String, b : String )  = {
      new WebsocketStringMGJ( storeUnitStr, a, List( b ) )
    }

    def loopBack( storeUnitStr : String ) = {
      ptToPt( storeUnitStr, "localhost", "localhost" )
    }

    import scala.collection.immutable.IndexedSeq
        
    type MsgTypes = DTSMSH[String,String,String,String]   
    
    val protoDreqUUID = getUUID()
    val protoDrspUUID = getUUID()    
    
    object MonadicDMsgs extends MsgTypes {
      
      override def protoDreq : DReq = MDGetRequest( aLabel )
      override def protoDrsp : DRsp = MDGetResponse( aLabel, aLabel.toString )
      override def protoJtsreq : JTSReq =
	JustifiedRequest(
	  protoDreqUUID,
	  new URI( "agent", protoDreqUUID.toString, "/invitation", "" ),
	  new URI( "agent", protoDreqUUID.toString, "/invitation", "" ),
	  getUUID(),
	  protoDreq,
	  None
	)
      override def protoJtsrsp : JTSRsp = 
	JustifiedResponse(
	  protoDreqUUID,
	  new URI( "agent", protoDrspUUID.toString, "/invitation", "" ),
	  new URI( "agent", protoDrspUUID.toString, "/invitation", "" ),
	  getUUID(),
	  protoDrsp,
	  None
	)
      override def protoJtsreqorrsp : JTSReqOrRsp =
	Left( protoJtsreq )
    }
    
    override def protoMsgs : MsgTypes = MonadicDMsgs
  }

}
