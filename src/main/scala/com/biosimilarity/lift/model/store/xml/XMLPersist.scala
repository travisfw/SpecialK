// -*- mode: Scala;-*- 
// Filename:    XMLPersist.scala 
// Authors:     lgm                                                    
// Creation:    Mon Jan 10 02:03:57 2011 
// Copyright:   Not supplied 
// Description: Deprecated for the xml db api calls
// ------------------------------------------------------------------------

//jsk - this has been refactored and split into BaseXPerist.scala, XMLIfy.scala, XMLStoreConfiguration.scala

package com.biosimilarity.lift.model.store.xml

import com.biosimilarity.lift.model.store._
import com.biosimilarity.lift.lib._

//trait XMLStoreDefaults
//
//trait XMLStore
//extends XMLStoreConfiguration {
//  self : UUIDOps =>
//
//    //override type ConfigurationDefaults = XMLStoreDefaults
//
//    //override def configFileName : Option[String] = Some(
//    //"xmlStore.conf" )
//    override def configFileName : Option[String] = None
//
//  def driverClass : Class[_] = {
//    Class.forName( driver )
//  }
//
//  def database : Database = {
//    val db = driverClass.newInstance().asInstanceOf[Database]
//    DatabaseManager.registerDatabase( db )
//    if ( createDB ) {
//      db.setProperty( "create-database", createDB.toString )
//    }
//    db
//  }
//
//  def getCollection( createIfMissing : Boolean )( xmlCollStr : String ) : Option[Collection] = {
//    val xmlColl : Option[Collection] =
//      {
//	val coll = DatabaseManager.getCollection( URI + xmlCollStr )
//	if ( coll == null ) {
//	  if ( createIfMissing ) {
//	    val root : Collection = DatabaseManager.getCollection( URI + dbRoot )
//	    val mgtService : CollectionManagementService =
//	      root.getService(
//		managementServiceType,
//		managementServiceVersion
//	      ).asInstanceOf[CollectionManagementService]
//
//	    Some(
//	      mgtService.createCollection(
//		xmlCollStr.substring( "/db".length() )
//	      )
//	    )
//	  }
//	  else {
//	    None
//	  }
//	}
//	else {
//	  Some( coll )
//	}
//      }
//
//    for( xColl <- xmlColl ) {
//      if ( indent ) {
//	xColl.setProperty(OutputKeys.INDENT, "yes")
//      }
//      else {
//	xColl.setProperty(OutputKeys.INDENT, "no")
//      }
//    }
//
//    xmlColl
//  }
//
//  def getResource( xmlColl : Collection )( xmlRsrcStr : String )
//  : Option[XMLResource] = {
//    val xmlRsrc =
//      xmlColl.getResource( xmlRsrcStr ).asInstanceOf[XMLResource]
//    if ( xmlRsrc == null ) {
//      None
//    }
//    else {
//      Some( xmlRsrc )
//    }
//  }
//
//  def createResource( xmlColl : Collection, xmlRsrcStr : String )
//  : Option[XMLResource] = {
//    val document : XMLResource =
//      xmlColl.createResource( null, resourceType ).asInstanceOf[XMLResource]
//
//    val f : File = new File( xmlRsrcStr )
//
//    if ( f.canRead() ) {
//      document.setContent( f )
//      xmlColl.storeResource( document )
//      Some( document )
//    }
//    else {
//      println( "cannot read file " + xmlRsrcStr )
//      None
//    }
//  }
//
//  def createResource( xmlColl : Collection, xmlRsrcFile : java.io.File )
//  : Option[XMLResource] = {
//
//    val document : XMLResource =
//      xmlColl.createResource( null, resourceType ).asInstanceOf[XMLResource]
//
//    if ( xmlRsrcFile.canRead() ) {
//      document.setContent( xmlRsrcFile )
//      xmlColl.storeResource( document )
//      Some( document )
//    }
//    else {
//      println( "cannot read file " + xmlRsrcFile.getName )
//      None
//    }
//  }
//
//  def createResourceFromContent(
//    xmlColl : Collection,
//    xmlRsrcContentStr : String
//  ) : Option[XMLResource] = {
//
//    val document : XMLResource =
//      xmlColl.createResource( null, resourceType ).asInstanceOf[XMLResource]
//
//    document.setContent( xmlRsrcContentStr )
//    xmlColl.storeResource( document )
//    Some( document )
//  }
//
//  def createResource( xmlColl : Collection, xmlRsrcContent : Elem )
//  : Option[XMLResource] = {
//
//    createResourceFromContent( xmlColl, xmlRsrcContent.toString )
//  }
//
//  def getQueryService( xmlColl : Collection )(
//    qrySrvcType : String, qrySrvcVersion : String
//  ) : Service = {
//    val srvc = xmlColl.getService( qrySrvcType, qrySrvcVersion )
//    // if ( indent ) {
////       srvc.setProperty( "indent", "yes" )
////     }
////     else {
////       srvc.setProperty( "indent", "no" )
////     }
//    srvc
//  }
//
//  def getQueryServiceC( xmlColl : Collection ) : Service = {
//    getQueryService( xmlColl )( queryServiceType, queryServiceVersion )
//  }
//
//  def execute( xmlColl : Collection )(
//    srvc : Service
//  )(
//    qry : String
//  ) : ResourceSet = {
//    srvc match {
//      // case xqSrvc : XQueryService => {
//// 	xqSrvc.execute( xqSrvc.compile( qry ) )
////       }
//      case xqSrvc : XPathQueryService => {
//	xqSrvc.query( qry )
//      }
//      case _ => {
//	throw new Exception( "execute not supported" )
//      }
//    }
//  }
//
//}

//trait CnxnStorage[Namespace,Var,Tag]
//extends XMLIfy[Namespace,Var] {
//  self : XMLStore
//    with UUIDOps =>
//
//    def tmpDirStr : String
//
//  def store( xmlCollStr : String )( cnxn : CnxnCtxtLabel[Namespace,Var,String] ) : Unit = {
//    import java.io.FileWriter
//    import java.io.BufferedWriter
//
//    for( xmlColl <- getCollection( true )( xmlCollStr ) ) {
//      val xmlRsrcId = getUUID()
//      val xmlRsrcStr = tmpDirStr + "/" + xmlRsrcId.toString + ".xml"
//      val xmlRsrcFile = new File( xmlRsrcStr )
//      val fstream : FileWriter = new FileWriter( xmlRsrcFile )
//      val out : BufferedWriter = new BufferedWriter( fstream )
//      //out.write( toXML( cnxn ) ) -- Serialization-based storage
//      out.write( xmlIfier.asXML( cnxn ).toString ) // Data-binding-based storage
//      out.close
//
//      val xrsrc = createResource( xmlColl, xmlRsrcFile )
//    }
//  }
//  def update( xmlCollStr : String )(
//    cnxn : CnxnCtxtLabel[Namespace,Var,String]
//  ) : Unit = {
//    store( xmlCollStr )( cnxn )
//  }
//
//}

//
//object CnxnStorageDefaults {
//  implicit val tmpDirStr = "tmp"
//}


