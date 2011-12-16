// -*- mode: Scala;-*- 
// Filename:    MonadicAMQP.scala 
// Authors:     lgm                                                    
// Creation:    Fri Jan 21 13:10:54 2011 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.lib

import com.biosimilarity.lift.lib.moniker._

import net.liftweb.amqp._

import scala.util.continuations._

import scala.concurrent.{Channel => Chan, _}
import scala.concurrent.cpsops._

import com.rabbitmq.client.{ Channel => RabbitChan, _}
import scala.actors.Actor

import scala.collection.Map

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
import java.io.ObjectInputStream
import java.io.ByteArrayInputStream
import java.util.Timer
import java.util.TimerTask

trait MonadicHTTPDispatcher[T]
 extends MonadicGenerators
  with FJTaskRunners {
   self : WireTap with Journalist =>          

     //  type ConnectionParameters = RabbitCnxnParams
     type Channel = HttpGet
    //  type Ticket = Int
    type Payload = HttpResponse

    trait ClientRequestPair[T] {
      def httpClient : HttpAsyncClient
      def channel : Channel
    }

    case class CnR[T](
      override val httpClient : HttpAsyncClient,
      override val channel : Channel
    ) extends ClientRequestPair[T]

    abstract class ResponseHandler[T](
      override val httpClient : HttpAsyncClient,
      override val channel : Channel
    ) extends FutureCallback[HttpResponse]
    with ClientRequestPair[T] {
      def completed( response : HttpResponse ) : Unit
      def failed( ex : Exception ) : Unit
      def cancelled() : Unit
    }

    def acceptConnections(
      factory : ClientConnectionManager,
      request : String
    ) =
    Generator {
      k : ( CnR[T] => Unit @suspendable ) => {
	//shift {
	  //innerk : (Unit => Unit @suspendable) => {	

	val httpClient = new DefaultHttpAsyncClient( factory )
	val channel = new HttpGet( request )

	httpClient.start

	k( CnR[T]( httpClient, channel ) );
	  //}
	//}      
      }
    }   

    def beginService(
      factory : ClientConnectionManager,
      request : String
    ) = {
      serve [T]( factory, request )
    }

   def serve [T] (
    factory : ClientConnectionManager,
    request : String
  ) = Generator {
    k : ( T => Unit @suspendable ) =>
      //shift {
	blog(
	  "The client is running... (don't let him get away!)"
	)

	for( cnr <- acceptConnections( factory, request ) ) {
	  spawn {
	    // Open bracket
	    blog( "Connected: " + cnr.channel )

            for ( t <- read [T] ( cnr.httpClient, cnr.channel ) ) { k( t ) }

            // Close bracket
	  }
	}
      //}
  }

  def callbacks( httpClient : HttpAsyncClient, channel : Channel ) =
    Generator {
      k : ( Payload => Unit @suspendable) =>

      blog("level 1 callbacks")

      shift {
	outerk : (Unit => Any) =>
	  
	  object TheRendezvous
	   extends ResponseHandler[T]( httpClient, channel ) {
    	     override def completed(
	       response : HttpResponse
	     ) {
    	       spawn { 
  		 blog("before continuation in callback")
  		
    		 k( response )
    		
    		 blog("after continuation in callback")
    		   
		 outerk()
    	       }
    	     }
	     override def failed( ex : Exception ) {
	       ex.printStackTrace()
	     }
	     override def cancelled() {
	       blog( "request cancelled" )
	     }
	   }
  	
  	blog("before registering callback")
  	
	httpClient.execute( channel, TheRendezvous )
  	
  	blog("after registering callback")
  	// stop
      }
    }   

    def dispatchContent [T] ( response : HttpResponse ) : T = {
      def entityContentString( entity : HttpEntity ) : String = {
	// BUGBUG -- lgm : should get the charset from the response	
	org.apache.commons.io.IOUtils.toString(
	  entity.getContent,
	  "UTF-8"
	)
      }
      val httpEntity = response.getEntity
      httpEntity.getContentType.getValue match {
	case "application/json" => {	  	  
	  val rslt =
	    new XStream(
	      new JettisonMappedXmlDriver()
	    ).fromXML( entityContentString( httpEntity ) )
	  rslt.asInstanceOf[T]
	}
	case "text/xml" => {
	  val rslt =
	    new XStream( ).fromXML( entityContentString( httpEntity ) )
	  rslt.asInstanceOf[T]
	}
	case "text/plain" => {
	  entityContentString( httpEntity ).asInstanceOf[T]
	}
	case ct@_ => {
	  throw new Exception( "content type (" + ct + ") not supported" )
	}
      }		 
      //val in = new ObjectInputStream( httpEntity.getContent )
      //val t = in.readObject.asInstanceOf[T];		 
    }

   def read [T] ( httpClient : HttpAsyncClient, channel : Channel ) =
     Generator {
       k: ( T => Unit @suspendable) =>
	 shift {
	   outerk: (Unit => Unit) =>
	     reset {
	      
  	       for (
		 response <- callbacks( httpClient, channel )
	       ) {
		 k( dispatchContent[T]( response ) )
		 // Is this necessary?
		 shift { k : ( Unit => Unit ) => k() }
  	       }
  	       
  	       blog( "readT returning" )
  	       outerk()
	     }
	 }
     }
   
 }

package usage {
/* ------------------------------------------------------------------
 * Mostly self-contained object to support unit testing
 * ------------------------------------------------------------------ */ 
  import org.apache.http.impl.conn.tsccm._
  import org.apache.http.impl.nio.reactor._
  import org.apache.http.impl.nio.client.DefaultHttpAsyncClient
  import org.apache.http.impl.nio.conn.PoolingClientConnectionManager  

  import java.io.StringReader  

  trait Argonaut {        
    import com.fasterxml.jackson.module.scala._
    import org.codehaus.jackson.map.ObjectMapper
    
    lazy val mapper = {
      val m = new ObjectMapper()
      m.registerModule(DefaultScalaModule)
      m
    }
    
    def toMap( json : String ) : scala.collection.Map[String,Object] = {
      mapper.readValue( json, classOf[scala.collection.Map[String,Object]] ).asInstanceOf[scala.collection.Map[String,Object]]
    }
    
    def getRspData [T] ( jsonRsp : String, key : String ) : Option[T] = {
      import scala.collection.JavaConverters._
      
      for(	
	rspData <- toMap( jsonRsp ).get( "data" );
	v <- rspData.asInstanceOf[java.util.LinkedHashMap[String,Object]].asScala.get( key )
      ) yield {
	v.asInstanceOf[T]
      }
    }
    
  }
  
  trait EtherpadLiteAPIData {
    val stdCaseClassMethods =
    List[String]( 
      "equals",
      "toString",
      "hashCode",
      "copy",
      "productPrefix",
      "productArity",
      "productElement",
      "productIterator",
      "productElements",
      "canEqual",
      "copy$default$1",
      "wait",
      "wait",
      "wait",
      "getClass",
      "notify",
      "notifyAll"
      )

    val baseURL = "http://beta.etherpad.org/api"
    val apiVersion = "1"
    val apiKey = "EtherpadFTW"

    trait EtherpadAPIMsg

    def toEtherpadVerb( msg : EtherpadAPIMsg ) : String = {
      val msgClassName = msg.getClass.getName
      val msgName =
	msgClassName.substring( 
	  msgClassName.indexOf( "$" ) + 1,
	  msgClassName.length 
	)
      
      msgName.take( 1 ).toLowerCase + msgName.drop( 1 )
    }
    def toEtherpadArgs( msg : EtherpadAPIMsg ) : String = {
      def getArg( msg : EtherpadAPIMsg, mthd : String ) : String = {
	val meth = msg.getClass.getMethod( mthd )
	meth.invoke( msg ).toString
      }

      ( "" /: msg.getClass.getMethods )(
	( acc, m ) => {
	  val methName = m.getName
	  if (
	    stdCaseClassMethods.contains( methName )
	    || methName.contains( "$$$outer" )
	    || methName.contains( "copy$default" )
	  ) {
	    acc
	  }
	  else {
	    acc + "&" + methName + "=" + getArg( msg, methName )
	  }
	}
      )
    }
    def toEtherpadRequest( msg : EtherpadAPIMsg ) : String = {
      (
	baseURL
	+ "/" + apiVersion
	+ "/" + toEtherpadVerb( msg )
	+ "?" + "apikey" + "=" + apiKey
	+ toEtherpadArgs( msg )
      )
    }

    case class CreateGroupIfNotExistsFor( groupMapper : String )
	 extends EtherpadAPIMsg

    case class CreateGroup( )
	 extends EtherpadAPIMsg
    case class DeleteGroup( groupID : String )
	 extends EtherpadAPIMsg
    case class ListPads( groupID : String )
	 extends EtherpadAPIMsg
    case class CreateGroupPad(
      groupID : String,
      padName: String,
      text : String
    ) extends EtherpadAPIMsg
    case class CreateAuthor( name : String ) extends EtherpadAPIMsg
    case class CreateAuthorIfNotExistsFor(
      authorMapper : String,
      name : String
    ) extends EtherpadAPIMsg
    case class CreateSession(
      groupID : String, authorID : String , validUntil : String
    ) extends EtherpadAPIMsg
    case class DeleteSession( sessionID : String ) extends EtherpadAPIMsg
    case class GetSessionInfo( sessionID : String ) extends EtherpadAPIMsg
    case class ListSessionsOfGroup( groupID : String ) extends EtherpadAPIMsg
    case class ListSessionsOfAuthor( authorID : String ) extends EtherpadAPIMsg
    case class GetText( padID : String ) extends EtherpadAPIMsg
    case class SetText( padID : String, text : String ) extends EtherpadAPIMsg
    case class CreatePad( padID : String, text : String ) extends EtherpadAPIMsg
    case class GetRevisionsCount( padID : String ) extends EtherpadAPIMsg
    case class GeletePad( padID : String ) extends EtherpadAPIMsg
    case class GetReadOnlyID( padID : String ) extends EtherpadAPIMsg
    case class SetPublicStatus( padID : String, publicStatus : String )
	 extends EtherpadAPIMsg
    case class GetPublicStatus( padID : String ) extends EtherpadAPIMsg
    case class SetPassword( padID : String, password : String )
	 extends EtherpadAPIMsg
    case class IsPasswordProtected( padID : String ) extends EtherpadAPIMsg
    
  }  

  object MndHTTPStringDispatcher
	 extends MonadicHTTPDispatcher[String]
	 with WireTap
	 with Journalist
	 with ConfiggyReporting
	 with ConfiggyJournal
  {
    lazy val dcior1 =
      new DefaultConnectingIOReactor()
    lazy val pccm =
      new PoolingClientConnectionManager( dcior1 )
    lazy val httpClient =
      {
	val httpC = new DefaultHttpAsyncClient( pccm );
	httpC.start
	httpC
      }
    
    override def dispatchContent [T] (
      response : HttpResponse
    ) : T = {
      org.apache.commons.io.IOUtils.toString(
	response.getEntity.getContent,
	"UTF-8"
      ).asInstanceOf[T]
    }
    
    object EtherpadLiteAPI extends EtherpadLiteAPIData
    {
      def apply(
	req : EtherpadAPIMsg,
	handler : String => Unit
      ) : Unit = {
	reset {
	  for(
	    rsp <- MndHTTPStringDispatcher.read [String] (
	      httpClient,
	      new HttpGet( toEtherpadRequest( req ) )
	    )
	  ) {
	    handler( rsp )
	  }
	}
      }
    }
    
    override def tap [A]( fact : A ) : Unit = {
      blog( fact )
    }	       
    
    object ComeOnOverToMyPad extends Argonaut {		 
      import java.security.SecureRandom
      import java.math.BigInteger
      import java.net.URLEncoder

      def randomSuffix() : String = {
	new BigInteger( 130, new SecureRandom() ).toString( 32 ).substring( 0, 10 )
      }

      def andHaveSomeFun() : Unit = {
	println( ">>>> sending CreateGroupIfNotExistsFor request >>>>" )
	EtherpadLiteAPI(
	  EtherpadLiteAPI.CreateGroupIfNotExistsFor(
	    "lgreg.meredith@gmail.com" 
	  ),
	  ( jsonRsp : String ) => {	      
	    println( "<<<< handling response to CreateGroupIfNotExistsFor request <<<<" )
	    for( groupID <- getRspData[String]( jsonRsp, "groupID" ) ) {
	      val padName = "myPad" + randomSuffix()	      
	      println( ">>>> sending CreateGroupPad request >>>>" )
	      EtherpadLiteAPI(
		EtherpadLiteAPI.CreateGroupPad(
		  groupID,
		  padName,
		  URLEncoder.encode( "lambda x.x" )
		),
		( jsonRsp : String ) => {    		    
		  println( "<<<< handling response to CreateGroupPad request <<<<" )
		  for( padID <- getRspData[String]( jsonRsp, "padID" ) ) {
		    println( ">>>> sending GetText request >>>>" )
		    EtherpadLiteAPI(
		      EtherpadLiteAPI.GetText( padID ),
		      ( jsonRsp : String ) => {
			println( "<<<< handling response to GetText request <<<<" )
			for( padText <- getRspData[String]( jsonRsp, "text" ) ) {
			  println(
			    "\nEtherpad( " + padID + " )"
			    + "\ncreated in group( " + groupID + " )"
			    + "\nwith contents: " + padText
			  )
			}
		      } 
		    )
		  }
		}
	      )
	    }
	  }
	)
      }
    }
  }  
  
}
