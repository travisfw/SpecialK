//// -*- mode: Scala;-*-
//// Filename:    LogMonads.scala
//// Authors:     lgm
//// Creation:    Fri Jul  1 10:53:28 2011
//// Copyright:   Not supplied
//// Description:
//// ------------------------------------------------------------------------
//
//package com.biosimilarity.lift.model.store.monitor
//
//import com.biosimilarity.lift.model.ApplicationDefaults
//
//import com.biosimilarity.lift.model.store._
//import com.biosimilarity.lift.model.store.xml._
//import com.biosimilarity.lift.model.agent._
//import com.biosimilarity.lift.model.msg._
//import com.biosimilarity.lift.lib._
//import com.biosimilarity.lift.lib.moniker._
//import com.biosimilarity.lift.lib.monad._
//
//import scala.concurrent.{Channel => Chan, _}
//import scala.concurrent.cpsops._
//import scala.util.continuations._
//import scala.xml._
//import scala.collection.MapProxy
//import scala.collection.mutable.Map
//import scala.collection.mutable.HashMap
//import scala.collection.mutable.LinkedHashMap
//import scala.collection.mutable.ListBuffer
//import scala.collection.mutable.MutableList
//
//import org.prolog4j._
//
////import org.exist.storage.DBBroker
//
//import org.xmldb.api.base.{ Resource => XmlDbRrsc, _}
//import org.xmldb.api.modules._
//import org.xmldb.api._
//
////import org.exist.util.serializer.SAXSerializer
////import org.exist.util.serializer.SerializerPool
//
//import com.thoughtworks.xstream.XStream
//import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver
//
//import javax.xml.transform.OutputKeys
//import java.util.Properties
//import java.net.URI
//import java.io.File
//import java.io.FileInputStream
//import java.io.OutputStreamWriter
//
//
//class SimpleStoreScope[A]( )
//    extends PersistedTermStoreScope[String,String,String,A] {
//      import SpecialKURIDefaults._
//      import CnxnLeafAndBranch._
//      import identityConversions._
//
//      type MTTypes = MonadicTermTypes[String,String,String,A]
//      object TheMTT extends MTTypes
//      override val protoTermTypes : MTTypes = TheMTT
//
//      type DATypes = DistributedAskTypes
//      object TheDAT extends DATypes
//      override def protoAskTypes : DATypes = TheDAT
//
//    class StringXMLDBManifest(
//	override val storeUnitStr : String,
//	override val labelToNS : Option[String => String],
//	override val textToVar : Option[String => String],
//	override val textToTag : Option[String => String]
//      )
//      //BUGBUG: factorization issues for getGV - usually part of the junction
//      extends MonadicTermStore
//      with XMLPersistManifest[mTT.GetRequest, mTT.Resource, Elem,  String, String, String, String ]
//      with CnxnXQuery[ String, String, String ]
//      with CnxnXML[ String, String, String ]
//      with Blobify
//      with CnxnCtxtInjector[ String, String, String ]
//      with XMLIfy[String,String]
//      with UUIDOps
//      with Journalist
//      //BUGBUG: factorization issues for valueStorageType?
//      with XMLStoreConfiguration
//      {
//          def kvNameSpace : String = "record"
//
//	  override def storeUnitStr[Src,Label,Trgt](
//	    cnxn : Cnxn[Src,Label,Trgt]
//	  ) : String = {
//	    cnxn match {
//	      case CCnxn( s, l, t ) =>
//		s.toString + l.toString + t.toString
//	    }
//	  }
//
//
//	  // BUGBUG -- LGM: Evidence of a problem with this factorization
//	  override def asCacheValue(
//	    ltns : String => String,
//	    ttv : String => String,
//	    value : Elem
//	  ) : Option[String] = {
//	    tweet(
//	      "Shouldn't be here!"
//	    )
//	    None
//	  }
//
//	  def asStoreValue(
//	    rsrc : mTT.Resource
//	  ) : CnxnCtxtLeaf[String,String,String] with Factual = {
//	    valueStorageType match {
//	      case "CnxnCtxtLabel" => {
//		tweet(
//		  "warning: CnxnCtxtLabel method is using XStream"
//		)
//
//		val blob = toXQSafeJSONBlob( rsrc )
//
//		new CnxnCtxtLeaf[String,String,String](
//		  Left[String,String](
//		    blob
//		  )
//		)
//	      }
//	      case "XStream" => {
//		tweet(
//		  "using XStream method"
//		)
//
//		val blob = toXQSafeJSONBlob( rsrc )
//
//		//asXML( rsrc )
//		new CnxnCtxtLeaf[String,String,String](
//		  Left[String,String]( blob )
//		)
//	      }
//	      case _ => {
//		throw new Exception( "unexpected value storage type" )
//	      }
//	    }
//	  }
//
//	  def asCacheValue(
//	    ccl : CnxnCtxtLabel[String,String,String]
//	  ) : String = {
//	    tweet(
//	      "converting to cache value"
//	    )
//	    //asPatternString( ccl )
//	    ccl match {
//	      case CnxnCtxtBranch(
//		"String",
//		CnxnCtxtLeaf( Left( rv ) ) :: Nil
//	      ) => {
//		val unBlob =
//		  fromXQSafeJSONBlob( rv ).asInstanceOf[A]
//
//		unBlob match {
//		  case rsrc : mTT.Resource => {
//		    getGV( rsrc ) match {
//		      case Some( cv: String ) => cv
//		      case _ =>
//			throw new Exception( "Missing resource conversion" )
//		    }
//		  }
//		}
//	      }
//	      case _ => {
//		//asPatternString( ccl )
//		throw new Exception( "Missing conversion" )
//	      }
//	    }
//	  }
//
//	}
//
//        def persistManifest( dfStoreUnitStr : String )  = {
//	  val sid = Some( ( s : String ) => s )
//	  new StringXMLDBManifest( dfStoreUnitStr, sid, sid, sid )
//	}
//
//      class PersistedStringMGJ(
//	val dfStoreUnitStr : String,
//	//override val name : URI,
//	override val name : Moniker,
//	//override val acquaintances : Seq[URI]
//	override val acquaintances : Seq[Moniker]
//      ) extends PersistedMonadicGeneratorJunction[StringXMLDBManifest](
//	name, acquaintances, persistManifest( dfStoreUnitStr )
//      ) {
//
//      }
//
//      def ptToPt( storeUnitStr : String, a : String, b : String )  = {
//	new PersistedStringMGJ( storeUnitStr, a, List( b ) )
//      }
//
//      def loopBack( storeUnitStr : String ) = {
//	ptToPt( storeUnitStr, "localhost", "localhost" )
//      }
//
//      import scala.collection.immutable.IndexedSeq
//
//      type MsgTypes = DTSMSH[String,String,String,A]
//
//      val protoDreqUUID = getUUID()
//      val protoDrspUUID = getUUID()
//
//      object MonadicDMsgs extends MsgTypes {
//
//	override def protoDreq : DReq = MDGetRequest( aLabel )
//	override def protoDrsp : DRsp = MDPutResponse( aLabel )
//	override def protoJtsreq : JTSReq =
//	  JustifiedRequest(
//	    protoDreqUUID,
//	    new URI( "agent", protoDreqUUID.toString, "/invitation", "" ),
//	    new URI( "agent", protoDreqUUID.toString, "/invitation", "" ),
//	    getUUID(),
//	    protoDreq,
//	    None
//	  )
//	override def protoJtsrsp : JTSRsp =
//	  JustifiedResponse(
//	    protoDreqUUID,
//	    new URI( "agent", protoDrspUUID.toString, "/invitation", "" ),
//	    new URI( "agent", protoDrspUUID.toString, "/invitation", "" ),
//	    getUUID(),
//	    protoDrsp,
//	    None
//	  )
//	override def protoJtsreqorrsp : JTSReqOrRsp =
//	  Left( protoJtsreq )
//      }
//
//      override def protoMsgs : MsgTypes = MonadicDMsgs
//    }
//
//
//class SimpleStore[A](
//  val storeUnitStr : String
//) extends SimpleStoreScope[A]( ) {
//  lazy val ss = loopBack( storeUnitStr )
//}
//
//// class SimpleStoreM[A](
////   val storeUnitStr : String,
////   val unitKey : CnxnCtxtLabel[String,String,String]
//// ) extends ForNotationAdapter[SimpleStore,A]
//// with BMonad[SimpleStore]
//// with MonadPlus[SimpleStore]
//// with MonadFilter[SimpleStore]
//// {
////   override def unit [S] ( s : S ) : SimpleStore[S] = {
////     val rslt = new SimpleStore[S]( storeUnitStr )
////     reset {
////       rslt.put( unitKey, s )
////     }
////     rslt
////   }
////   override def bind [S,T] (
////     sss : SimpleStore[S],
////     f : S => SimpleStore[T]
////   ) : SimpleStore[T] = {
////     val rslt = new SimpleStore[T]( storeUnitStr )
////   }
//// }
