// -*- mode: Scala;-*- 
// Filename:    PersistedMonadicTermStore.scala 
// Authors:     lgm                                                    
// Creation:    Fri Mar 18 15:04:22 2011 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.model.store

import com.biosimilarity.lift.model.ApplicationDefaults
import com.biosimilarity.lift.model.store.xml._
import com.biosimilarity.lift.model.agent._
import com.biosimilarity.lift.model.msg._
import com.biosimilarity.lift.lib._
import com.biosimilarity.lift.lib.moniker._

import scala.concurrent.{Channel => Chan, _}
import scala.concurrent.cpsops._
import scala.util.continuations._ 
import scala.xml._
import scala.collection.MapProxy
import scala.collection.mutable.Map
import scala.collection.mutable.HashMap
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.MutableList

import org.prolog4j._

//import org.exist.storage.DBBroker

import org.xmldb.api.base.{ Resource => XmlDbRrsc, _}
import org.xmldb.api.modules._
import org.xmldb.api._

//import org.exist.util.serializer.SAXSerializer
//import org.exist.util.serializer.SerializerPool

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver

import javax.xml.transform.OutputKeys
import java.util.Properties
import java.net.URI
import java.io.File
import java.io.FileInputStream
import java.io.OutputStreamWriter

trait PersistedTermStoreScope[Namespace,Var,Tag,Value] 
extends MonadicTermStoreScope[Namespace,Var,Tag,Value] {

//  trait PersistenceManifest {
//    def db : Database
//    def storeUnitStr[Src,Label,Trgt]( cnxn : Cnxn[Src,Label,Trgt] ) : String
//    def storeUnitStr : String
//    //deprecated
////    def toFile( ptn : mTT.GetRequest ) : Option[File]
//    def query( ptn : mTT.GetRequest ) : Option[String]
//    def query( xmlCollStr : String, ptn : mTT.GetRequest ) : Option[String]
//
//    def labelToNS : Option[String => Namespace]
//    def textToVar : Option[String => Var]
//    def textToTag : Option[String => Tag]
//
//    def kvNameSpace : Namespace
//
////    def asStoreKey(
////      key : mTT.GetRequest
////    ) : CnxnCtxtLabel[Namespace,Var,String] with Factual
//
//    def asStoreValue(
//      rsrc : mTT.Resource
//    ) : CnxnCtxtLeaf[Namespace,Var,String] with Factual
//
//    def asStoreRecord(
//      key : mTT.GetRequest,
//      value : mTT.Resource
//    ) : CnxnCtxtLabel[Namespace,Var,String] with Factual
//
//    def asCacheValue(
//      ccl : CnxnCtxtLabel[Namespace,Var,String]
//    ) : Value
//
//    def asCacheValue(
//      ltns : String => Namespace,
//      ttv : String => Var,
//      value : Elem
//    ) : Option[Value]
//
//    def asResource(
//      key : mTT.GetRequest, // must have the pattern to determine bindings
//      value : Elem
//    ) : Option[mTT.Resource]
//
//    def recordDeletionQueryTemplate : String = {
//      "delete node let $key := %RecordKeyConstraints% for $rcrd in collection( '%COLLNAME%' )//record where deep-equal($rcrd/*[1], $key) return $rcrd"
//    }
//  }

//  trait PersistenceManifestTrampoline {
//    def persistenceManifest : Option[PersistenceManifest]
//
//    def storeUnitStr[Src,Label,Trgt](
//      cnxn : Cnxn[Src,Label,Trgt]
//    ) : Option[String] = {
//      for( pd <- persistenceManifest )
//	yield {
//	  pd.storeUnitStr( cnxn )
//	}
//    }
//
//    def storeUnitStr : Option[String] = {
//      for( pd <- persistenceManifest )
//	yield {
//	  pd.storeUnitStr
//	}
//    }
//
//    def query( ptn : mTT.GetRequest ) : Option[String] = {
//      for( pd <- persistenceManifest; qry <- pd.query( ptn ) )
//	yield {
//	  qry
//	}
//    }
//
//    def query( xmlCollStr : String, ptn : mTT.GetRequest ) : Option[String] = {
//      for( pd <- persistenceManifest; qry <- pd.query( xmlCollStr, ptn ) )
//	yield {
//	  qry
//	}
//    }
//
//    def labelToNS : Option[String => Namespace] = {
//      for( pd <- persistenceManifest; ltns <- pd.labelToNS )
//	yield {
//	  ltns
//	}
//    }
//    def textToVar : Option[String => Var] = {
//      for( pd <- persistenceManifest; ttv <- pd.textToVar )
//	yield {
//	  ttv
//	}
//    }
//    def textToTag : Option[String => Tag] = {
//      for( pd <- persistenceManifest; ttt <- pd.textToTag )
//	yield {
//	  ttt
//	}
//    }
//
//    def kvNameSpace : Option[Namespace] = {
//      for( pd <- persistenceManifest )
//	yield { pd.kvNameSpace }
//    }
//
//    def asStoreKey(
//      key : mTT.GetRequest
//    ) : Option[CnxnCtxtLabel[Namespace,Var,String] with Factual] = {
//      for( pd <- persistenceManifest )
//	yield { pd.asStoreKey( key ) }
//    }
//
//    def asStoreValue(
//      rsrc : mTT.Resource
//    ) : Option[CnxnCtxtLeaf[Namespace,Var,String] with Factual] = {
//      for( pd <- persistenceManifest )
//	yield { pd.asStoreValue( rsrc ) }
//    }
//
//    def asStoreRecord(
//      key : mTT.GetRequest,
//      value : mTT.Resource
//    ) : Option[CnxnCtxtLabel[Namespace,Var,String] with Factual] = {
//      for( pd <- persistenceManifest )
//	yield { pd.asStoreRecord( key, value ) }
//    }
//
//    def asResource(
//      key : mTT.GetRequest, // must have the pattern to determine bindings
//      value : Elem
//    ) : Option[mTT.Resource] = {
//      for( pd <- persistenceManifest; rsrc <- pd.asResource( key, value ) )
//	yield { rsrc }
//    }
//
//    def asCacheValue(
//      ccl : CnxnCtxtLabel[Namespace,Var,String]
//    ) : Option[Value] = {
//      for( pd <- persistenceManifest )
//	yield { pd.asCacheValue( ccl ) }
//    }
//
//    def asCacheValue(
//      ltns : String => Namespace,
//      ttv : String => Var,
//      value : Elem
//    ) : Option[Value] = {
//      for(
//	pd <- persistenceManifest;
//	rsrc <- pd.asCacheValue( ltns, ttv, value )
//      )	yield { rsrc }
//    }
//  }

  //see if db can be deprecated, only used in unapply equality?
//  abstract class XMLDBManifest(
//    override val db : Database
//  ) extends XMLPersistManifest[mTT.GetRequest, mTT.Resource, Elem,  Namespace,Var,Tag,Value]
//  with CnxnXQuery[Namespace,Var,Tag]
//  with CnxnXML[Namespace,Var,Tag]
//  with CnxnCtxtInjector[Namespace,Var,Tag]
//  with XMLIfy[Namespace,Var]
//  with Blobify
//  with UUIDOps {
//
////    override val mTTScope = mTT
//
//    // BUGBUG -- LGM: Why not just the identity?
//
//
//  }
//  object XMLDBManifest {
//    def unapply(
//      ed : XMLDBManifest
//    ) : Option[( Database )] = {
//      Some( ( ed.db ) )
//    }
//  }
  
  abstract class PersistedMonadicGeneratorJunction[T <: XMLPersistManifest[mTT.GetRequest, mTT.Resource, Elem, Namespace,Var,Tag,Value]](
    //override val name : URI,
    override val name : Moniker,
    //override val acquaintances : Seq[URI]
    override val acquaintances : Seq[Moniker],
    val persistManifest : T
  ) extends DistributedMonadicGeneratorJunction(
    name,
    acquaintances
  ) with BaseXXMLStore
    with BaseXCnxnStorage[Namespace,Var,Tag]
  {
    //passed into each method
    //def persistenceManifest : Option[PersistManifest[mTT.GetRequest, mTT.Resource, Elem, Namespace,Var,Tag,Value]]

    override def tmpDirStr : String = {
      val tds = config.getString( "storageDir", "tmp" )       
      val tmpDir = new java.io.File( tds )
      if ( ! tmpDir.exists ) {
	tmpDir.mkdir
      }
      tds
    }

    override def configurationDefaults : ConfigurationDefaults = {
      ApplicationDefaults.asInstanceOf[ConfigurationDefaults]
    } 
    
    // BUGBUG -- LGM: Further evidence of problem with current factorization...
//    override def asResource(
//      key : mTT.GetRequest, // must have the pattern to determine bindings
//      value : Elem
//    ) : Option[mTT.Resource] = {
//      for(
//	ltns <- labelToNS;
//	ttv <- textToVar;
//	vCCL <- asCacheValue( ltns, ttv, value )
//      ) yield {
//	// BUGBUG -- LGM need to return the Solution
//	// Currently the PersistenceManifest has no access to the
//	// unification machinery
//	mTT.RBound(
//	  Some( mTT.Ground( vCCL ) ),
//	  None
//	)
//      }
//    }//

//    def asCursor//(
//      values : List[mTT.Resource//]
//    ) : Option[mTT.Resource] = //{//
//
//      val ig : mTT.Generator[mTT.Resource, Unit, Unit]  = mTT.itergen[mTT.Resource]( values //)//
//
//	// BUGBUG -- LGM need to return the Solutio//n
//	// Currently the PersistenceManifest has no access to th//e
//	// unification machiner//y
//	Some //(
//          mTT.RBound//(
//	    Some( mTT.Cursor( ig ) )//,
//	    Non//e
//  	  //)
//        //)
//    }


    //todo: decide where to override this method
//    override def asCacheValue(
//      ltns : String => Namespace,
//      ttv : String => Var,
//      value : Elem
//    ) : Option[Value] = {
//      tweet(
//	"converting store value to cache value"
//      )
//      valueStorageType match {
//	case "CnxnCtxtLabel" => {
//	  tweet(
//	    "using CnxnCtxtLabel method"
//	  )
//	  val ttt = ( x : String ) => x
//	  xmlIfier.fromXML( ltns, ttv, ttt )( value ) match {
//	    case Some( CnxnCtxtBranch( ns, k :: v :: Nil ) ) => {
//	      tweet(
//		"Good news! Value has the shape of a record"
//	      )
//	      if ( kvNameSpace.getOrElse( "" ).equals( ns ) ) {
//		tweet(
//		  "namespace matches : " + ns
//		)
//		tweet(
//		  "value before conversion is \n" + v
//		)
//		for(
//		  vale <-
//		  asCacheValue(
//		    v.asInstanceOf[CnxnCtxtLabel[Namespace,Var,String]]
//		  )
//		) yield { vale }
//	      }
//	      else {
//		tweet(
//		  "namespace mismatch: " + kvNameSpace + "," + ns
//		)
//		None
//	      }
//	    }
//	    case _ => {
//	      tweet(
//		"Value failed to embody the shape of a record" + value
//	      )
//	      None
//	    }
//	  }
//	}
//	case "XStream" => {
//	  tweet(
//	    "using XStream method"
//	  )
//	  xmlIfier.fromXML( ltns, ttv )( value ) match {
//	    case Some( CnxnCtxtBranch( ns, k :: v :: Nil ) ) => {
//	      v match {
//		case CnxnCtxtLeaf( Left( t ) ) => {
//		  Some(
//		    new XStream(
//		      new JettisonMappedXmlDriver
//		    ).fromXML( t ).asInstanceOf[Value]
//		  )
//		}
//		case _ => None
//	      }
//	    }
//	    case v@_ => {
//	      None
//	    }
//	  }
//	}
//	case _ => {
//	  throw new Exception( "unexpected value storage type" )
//	}
//      }
//    }



    def putInStore(
      persist : Option[PersistManifest[mTT.GetRequest, mTT.Resource, Elem, Namespace,Var,Tag,Value]],
      channels : Map[mTT.GetRequest,mTT.Resource],
      ptn : mTT.GetRequest,
      wtr : Option[mTT.GetRequest],
      rsrc : mTT.Resource,
      collName : Option[String]
    ) : Unit = {
      persist match {
	case None => {
	  channels( wtr.getOrElse( ptn ) ) = rsrc	  
	}
	case Some( pd: PersistManifest[_, _, Elem, Namespace,Var,Tag,Value] ) => {
//	  tweet( "accessing db : " + pd.db )
          tweet( "accessing db : " )

	  // remove this line to force to db on get
	  channels( wtr.getOrElse( ptn ) ) = rsrc	  
	  spawn {
            //BUGBUG: typing issues with the for extractor
            val rcrd = pd.asStoreRecord( ptn, rsrc )
            val sus = collName.getOrElse("")
//	    for(
//	      rcrd: CnxnCtxtLabel[Namespace,Var,String] with Factual <- pd.asStoreRecord( ptn, rsrc );
//	      sus: String <- collName
//	    ) {

	      tweet(
		(
//		  "storing to db : " + pd.db
                  "storing to db : "
		  + " pair : " + rcrd
		  + " in coll : " + sus
		)
	      )
	      store( sus )( rcrd )
//	    }
	  }
	}
      }
    }

    def putPlaces( persist : Option[PersistManifest[mTT.GetRequest, mTT.Resource, Elem, Namespace,Var,Tag,Value]] )(
      channels : Map[mTT.GetRequest,mTT.Resource],
      registered : Map[mTT.GetRequest,List[RK]],
      ptn : mTT.GetRequest,
      rsrc : mTT.Resource,
      collName : Option[String]
    ) : Generator[PlaceInstance,Unit,Unit] = {    
      Generator {
	k : ( PlaceInstance => Unit @suspendable ) => 
	  // Are there outstanding waiters at this pattern?    
	  val map =
	    Right[
	      Map[mTT.GetRequest,mTT.Resource],
	      Map[mTT.GetRequest,List[RK]]
	    ]( registered )
	  val waitlist = locations( map, ptn )

	  waitlist match {
	    // Yes!
	    case waiter :: waiters => {
	      tweet( "found waiters waiting for a value at " + ptn )
	      val itr = waitlist.toList.iterator	    
	      while( itr.hasNext ) {
		k( itr.next )
	      }
	    }
	    // No...
	    case Nil => {
	      // Store the rsrc at a representative of the ptn
	      tweet( "no waiters waiting for a value at " + ptn )
	      //channels( representative( ptn ) ) = rsrc
	      putInStore(
		persist, channels, ptn, None, rsrc, collName
	      )
	    }
	  }
      }
    }
    
    def mput( persist : Option[PersistManifest[mTT.GetRequest, mTT.Resource, Elem, Namespace,Var,Tag,Value]] )(
      channels : Map[mTT.GetRequest,mTT.Resource],
      registered : Map[mTT.GetRequest,List[RK]],
      consume : Boolean,
      collName : Option[String]
    )(
      ptn : mTT.GetRequest,
      rsrc : mTT.Resource
    ) : Unit @suspendable = {    
      for(
	placeNRKsNSubst
	<- putPlaces(
	  persist
	)( channels, registered, ptn, rsrc, collName )
      ) {
	val PlaceInstance( wtr, Right( rks ), s ) = placeNRKsNSubst
	tweet( "waiters waiting for a value at " + wtr + " : " + rks )
	rks match {
	  case rk :: rrks => {	
	    if ( consume ) {
	      for( sk <- rks ) {
		spawn {
		  sk( s( rsrc ) )
		}
	      }
	    }
	    else {
	      registered( wtr ) = rrks
	      rk( s( rsrc ) )
	    }
	  }
	  case Nil => {
	    putInStore(
	      persist, channels, ptn, Some( wtr ), rsrc, collName
	    )
	  }
	}
      }
      
    }

    def mget(
      persist : Option[PersistManifest[mTT.GetRequest, mTT.Resource, Elem, Namespace,Var,Tag,Value]],
      ask : dAT.Ask,
      //hops : List[URI]
      hops : List[Moniker]
    )(
      channels : Map[mTT.GetRequest,mTT.Resource],
      registered : Map[mTT.GetRequest,List[RK]],
      consume : Boolean,
      cursor : Boolean,
      collName : Option[String]
    )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {        
      Generator {	
	rk : ( Option[mTT.Resource] => Unit @suspendable ) =>
	  shift {
	    outerk : ( Unit => Unit ) =>
	      reset {
		for(
		  oV <- mget( channels, registered, consume )( path ) 
		) {
		  oV match {
		    case None => {
		      persist match {
			case None => {
			  tweet( ">>>>> forwarding..." )
			  forward( ask, hops, path )
			  rk( oV )
			}
			case Some( pd ) => {
			  tweet(
//                            "accessing db : " + pd.db
			    "accessing db : "
			  )

			  val xmlCollName =
			    collName.getOrElse(
                            pd.storeUnitStr
                            //should be pd.storeUnitStr...
//                              storeUnitStr.getOrElse(
//			      pd.storeUnitStr.getOrElse(
//				bail()
//			      )
			    )

			  // Defensively check that db is actually available
			  
			  checkIfDBExists( xmlCollName, true ) match {
			    case true => {
                              //issue with casting pd, using persist.map
			      val oQry = pd.query( xmlCollName, path )
			  
			      oQry match {
				case None => {
				  tweet( ">>>>> forwarding..." )
				  forward( ask, hops, path )
				  rk( oV )
				}
				case Some( qry ) => {	
				  tweet(
				    (
				      "retrieval query : \n" + qry
				    )
				  )
				
				  val rslts = executeWithResults( qry )
				
				  rslts match {
				    case Nil => {	
				      tweet(
					(
					  "database "
					  + xmlCollName
					  + " had no matching resources."
					)
				      )
				      forward( ask, hops, path )
				      rk( oV )
				    }
				    case _ => { 			  
				      tweet(
					(
					  "database "
					  + xmlCollName
					  + " had "
					  + rslts.length
					  + " matching resources."
					)
				      )		  				  
				  
				      if ( cursor )
                                      {
                                        var rsrcRslts : List[mTT.Resource] = Nil
                                        for( rslt <- itergen[Elem]( rslts ) ) {
                                          tweet( "retrieved " + rslt.toString )

                                          if ( consume ) {
                                            tweet( "removing from store " + rslt )
                                            removeFromStore(
                                              persist,
                                              rslt,
                                              collName
                                            )
                                          }

                                          // BUGBUG -- LGM : This is a
                                          // window of possible
                                          // failure; if we crash here,
                                          // then the result is out of
                                          // the store, but we haven't
                                          // completed processing. This is
                                          // where we need Tx.
//                                          val ersrc : Option[mTT.Resource] = mTT.asResource( path, pd.asValue(rslt) )
                                          val ersrc : Option[mTT.Resource] = mTT.asResource( pd.asValue(rslt) )
                                          ersrc match {
                                            case Some(r) => rsrcRslts = r :: rsrcRslts
                                            case _ => {}
                                          }

                                        }

                                        val rsrcCursor = mTT.asCursor( rsrcRslts )
                                        //tweet( "returning cursor" + rsrcCursor )
                                        rk( rsrcCursor )
                                      }
                                      else
                                      {
					for( rslt <- itergen[Elem]( rslts ) ) {
					  tweet( "retrieved " + rslt.toString )
//                                          val ersrc = mTT.asResource( path, pd.asValue(rslt) )
					  val ersrc = mTT.asResource( pd.asValue(rslt) )
					  
					  if ( consume ) {
					    tweet( "removing from store " + rslt )
					    removeFromStore( 
					      persist,
					      rslt,
					      collName
					    )
					  }
					  
					  // BUGBUG -- LGM : This is a
					  // window of possible
					  // failure; if we crash here,
					  // then the result is out of
					  // the store, but we haven't
					  // completed processing. This is
					  // where we need Tx.
					  tweet( "returning " + ersrc )
					  rk( ersrc )
					}
				      }
				    }
				  }
				}			    
			      }			      
			    }
			    case false => {
			      tweet( ">>>>> forwarding..." )
			      forward( ask, hops, path )
			      rk( oV )
			    }
			  }
			}		      
		      }
		    }
		    case _ => rk( oV )
		  }
		}
	      }
	  }
      }
    }

    def removeFromStore(
      persist : Option[PersistManifest[mTT.GetRequest, mTT.Resource, Elem, Namespace,Var,Tag,Value]],
      record : Elem,
      collName : Option[String]
    ) : Unit = {
      for( pd <- persist; clNm <- collName )
	{
//          tweet( "removing from db : " + pd.db )
          tweet( "removing from db : " + collName )
	  val rcrdKey =
	    record match {
	      case <record>{ kv @_* }</record> => {
		val kvs = kv.toList.filter(
		  ( n : Node ) => {
		    n match {
		      case Text( contents ) => {
			!java.util.regex.Pattern.matches( 
			      "(\\p{Space}|\\p{Blank})*",
			      contents
			)
		      }
		      case e : Elem => {
			true
		      }
		    }
		  }
		)
		kvs match {
		  case k :: v :: Nil => k.toString
		  case _ => 
		    throw new Exception(
		      "Not a k-v record shape\n" + kvs
		    )
		}
	      }
	      case _ =>
		throw new Exception(
		  "Not a record\n" + record
		)
	    }

	  val deletionQry =
	    pd.recordDeletionQueryTemplate.replace(
	      "%RecordKeyConstraints%",
	      rcrdKey
	    ).replace(
	      "%COLLNAME%",
	      clNm
	    )
	  tweet( "deletion query : \n" + deletionQry )
	  val ostrm = new java.io.ByteArrayOutputStream()
	  execute( List( deletionQry ) )
	  tweet(
	    "deletion results: \n" + ostrm.toString
	  )
	}
    }

    override def put(
      ptn : mTT.GetRequest, rsrc : mTT.Resource
    ) = {
      val xmlCollName = Some( persistManifest.storeUnitStr )
      mput( Some( persistManifest ) )(
	theMeetingPlace, theWaiters, false, xmlCollName
      )( ptn, rsrc )
    }
    override def publish(
      ptn : mTT.GetRequest, rsrc : mTT.Resource
    ) = {
      val xmlCollName = Some( persistManifest.storeUnitStr )
      mput( Some( persistManifest ) )(
	theChannels, theSubscriptions, true, xmlCollName
      )( ptn, rsrc )
    }

    //override def get( hops : List[URI] )(
    override def get( hops : List[Moniker] )(
      cursor: Boolean
      )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {        
      val xmlCollName = Some( persistManifest.storeUnitStr )
      mget( Some( persistManifest), dAT.AGet, hops )(
	theMeetingPlace, theWaiters, true, cursor, xmlCollName
      )( path )    
    }
    override def get(
      cursor: Boolean
      )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {
      get( Nil )( cursor )( path )
    }
    override def get(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {        
      get( Nil )( false )( path )    
    }

    //override def fetch( hops : List[URI] )(
    override def fetch( hops : List[Moniker] )(
      cursor: Boolean
      )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {        
      val xmlCollName = Some( persistManifest.storeUnitStr )
      mget( Some( persistManifest ), dAT.AFetch, hops )(
	theMeetingPlace, theWaiters, false, cursor, xmlCollName
      )( path )    
    }
    override def fetch(
      cursor: Boolean
      )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {
      fetch( Nil )( cursor )( path )
    }
    override def fetch(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {        
      fetch( Nil )( false )( path )    
    }

    //override def subscribe( hops : List[URI] )(
    override def subscribe( hops : List[Moniker] )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {        
      val xmlCollName = Some( persistManifest.storeUnitStr )
      mget( Some( persistManifest ), dAT.ASubscribe, hops )(
	theChannels, theSubscriptions, true, false, xmlCollName
      )( path )    
    }
    override def subscribe(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {        
      subscribe( Nil )( path )    
    }
  }
}


package usage {
/* ------------------------------------------------------------------
 * Mostly self-contained object to support unit testing
 * ------------------------------------------------------------------ */ 

object PersistedMonadicTS
 extends PersistedTermStoreScope[String,String,String,String] 
  with UUIDOps {
    import SpecialKURIDefaults._
    import CnxnLeafAndBranch._
    import identityConversions._

    type MTTypes = MonadicTermTypes[String,String,String,String]
    object TheMTT extends MTTypes
    override val protoTermTypes : MTTypes = TheMTT

    type DATypes = DistributedAskTypes
    object TheDAT extends DATypes
    override def protoAskTypes : DATypes = TheDAT

  class StringXMLDBManifest(
	override val storeUnitStr : String,
	override val labelToNS : Option[String => String],
	override val textToVar : Option[String => String],
	override val textToTag : Option[String => String]
      )
      //BUGBUG: factorization issues for getGV - usually part of the junction
      extends XMLPersistManifest[mTT.GetRequest, mTT.Resource, Elem,  String, String, String, String ]
      with CnxnXQuery[ String, String, String ]
      with CnxnXML[ String, String, String ]
      with Blobify
      with CnxnCtxtInjector[ String, String, String ]
      with XMLIfy[String,String]
      with UUIDOps
      with Journalist
      //BUGBUG: factorization issues for valueStorageType?
      with XMLStoreConfiguration
      {
        //BUGBUG: factorization issues by including XMLStoreConfiguration?
        override def configFileName : Option[String] = None

        def kvNameSpace : String = "record"

	override def storeUnitStr[Src,Label,Trgt](
	  cnxn : Cnxn[Src,Label,Trgt]
	) : String = {
	  cnxn match {
	    case CCnxn( s, l, t ) =>
	      s.toString + l.toString + t.toString
	  }
	}

	def asStoreValue(
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
		 mTT.asGroundValue( rsrc ).getOrElse( "" )
		}
	      }
	    }
	    case _ => {
	      asPatternString( ccl )
	    }
	  }
	}

      }

  def persistManifest(dfStoreUnitStr: String) =
  {
    val sid = Some((s: String) => s)
    new StringXMLDBManifest(dfStoreUnitStr, sid, sid, sid)
  }

    class PersistedStringMGJ(
      val dfStoreUnitStr : String,
      //override val name : URI,
      override val name : Moniker,
      //override val acquaintances : Seq[URI]
      override val acquaintances : Seq[Moniker]
    ) extends PersistedMonadicGeneratorJunction[StringXMLDBManifest](
      name, acquaintances, persistManifest(dfStoreUnitStr)
    ) {

	// BUGBUG -- LGM: Evidence of a problem with this factorization
//	override def asCacheValue(
//	  ltns : String => String,
//	  ttv : String => String,
//	  value : Elem
//	) : Option[String] = {
//	  tweet(
//	    "Shouldn't be here!"
//	  )
//	  None
//	}

    }
    
    def ptToPt( storeUnitStr : String, a : String, b : String )  = {
      new PersistedStringMGJ( storeUnitStr, a, List( b ) )
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

  //BUGBUG: commented out for now
//object StdPersistedMonadicTS
// extends PersistedTermStoreScope[Symbol,Symbol,Any,Any]
//  with UUIDOps {
//    import SpecialKURIDefaults._
//    import CnxnLeafAndBranch._
//    import CCLDSL._
//    import identityConversions._
//
//    type MTTypes = MonadicTermTypes[Symbol,Symbol,Any,Any]
//    object TheMTT extends MTTypes
//    override val protoTermTypes : MTTypes = TheMTT
//
//    type DATypes = DistributedAskTypes
//    object TheDAT extends DATypes
//    override def protoAskTypes : DATypes = TheDAT
//
//    class StringXMLDBManifest(
//	override val storeUnitStr : String,
//	override val labelToNS : Option[String => Symbol],
//	override val textToVar : Option[String => Symbol],
//	override val textToTag : Option[String => Any]
//      )
//      //BUGBUG: factorization issues for getGV - usually part of the junction
//      extends MonadicTermStore
//      with XMLPersistManifest[mTT.GetRequest, mTT.Resource, Elem,  String, Symbol, Symbol, Any ]
//      with CnxnXQuery[ String, Symbol, Symbol ]
//      with CnxnXML[ String, Symbol, Symbol ]
//      with Blobify
//      with CnxnCtxtInjector[ String, Symbol, Symbol ]
//      with XMLIfy[ String, Symbol ]
//      with UUIDOps
//      with Journalist
//      //BUGBUG: factorization issues for valueStorageType?
//      with XMLStoreConfiguration
//    {
//	override def storeUnitStr[Src,Label,Trgt](
//	  cnxn : Cnxn[Src,Label,Trgt]
//	) : String = {
//	  cnxn match {
//	    case CCnxn( s, l, t ) =>
//	      s.toString + l.toString + t.toString
//	  }
//	}
//
//// BUGBUG -- LGM: Evidence of a problem with this factorization
//        def asCacheValue(
//          ltns : String => Symbol,
//          ttv : String => Symbol,
//          value : Elem
//        ) : Option[String] = {
//          tweet(
//            "Shouldn't be here!"
//          )
//          None
//        }
//
//        def asStoreValue(
//          rsrc : mTT.Resource
//        ) : CnxnCtxtLeaf[Symbol,Symbol,String] with Factual = {
//          valueStorageType match {
//            case "CnxnCtxtLabel" => {
//              tweet(
//                "warning: CnxnCtxtLabel method is using XStream"
//              )
//
//              val blob = toXQSafeJSONBlob( rsrc )
//
//              new CnxnCtxtLeaf[Symbol,Symbol,String](
//                Left[String,Symbol](
//                  blob
//                )
//              )
//            }
//            case "XStream" => {
//              tweet(
//                "using XStream method"
//              )
//              val blob = toXQSafeJSONBlob( rsrc )
//              //asXML( rsrc )
//              new CnxnCtxtLeaf[Symbol,Symbol,String](
//                Left[String,Symbol]( blob )
//              )
//            }
//            case _ => {
//              throw new Exception( "unexpected value storage type" )
//            }
//          }
//        }
//
//        def asCacheValue(
//          ccl : CnxnCtxtLabel[Symbol,Symbol,String]
//        ) : String = {
//          tweet(
//            "converting to cache value"
//          )
//          //asPatternString( ccl )
//          ccl match {
//            case CnxnCtxtBranch(
//              storeType,
//              CnxnCtxtLeaf( Left( rv ) ) :: Nil
//            ) => {
//              def extractValue( rv : String ) : String = {
//                val unBlob =
//                  fromXQSafeJSONBlob( rv )
//
//                unBlob match {
//                  case rsrc : mTT.Resource => {
//                    (getGV( rsrc ).getOrElse( "" ) + "")
//                  }
//                }
//              }
//
//              (storeType + "") match {
//                case "String" => {
//                  extractValue( rv )
//                }
//                case "'String" => {
//                  extractValue( rv )
//                }
//              }
//            }
//            case _ => {
//              asPatternString(
//              //BUGBUG: jsk - typing issue?
////                ccl.asInstanceOf[CnxnCtxtLabel[Symbol,Symbol,Any]]
//                ccl.asInstanceOf[CnxnCtxtLabel[String,Symbol,Symbol]]
//              )
//            }
//          }
//        }
//
//      }
//
//  def persistManifest(dfStoreUnitStr: String)=
//  {
//    val sid = Some((s: String) => s)
//    val sym = Some((s: String) => Symbol(s))
//    new StringXMLDBManifest(dfStoreUnitStr, sym, sym, sid)
//  }
//
//    class PersistedStdMGJ(
//      val dfStoreUnitStr : String,
//      //override val name : URI,
//      override val name : Moniker,
//      //override val acquaintances : Seq[URI]
//      override val acquaintances : Seq[Moniker]
//    ) extends PersistedMonadicGeneratorJunction[StringXMLDBManifest](
//      name, acquaintances, persistManifest(dfStoreUnitStr)
//    ) {
//
//      def kvNameSpace : Symbol = 'record
//
//    }
//
//    def ptToPt( storeUnitStr : String, a : String, b : String )  = {
//      new PersistedStdMGJ( storeUnitStr, a, List( b ) )
//    }
//
//    def loopBack( storeUnitStr : String ) = {
//      ptToPt( storeUnitStr, "localhost", "localhost" )
//    }
//
//    import scala.collection.immutable.IndexedSeq
//
//    type MsgTypes = DTSMSH[Symbol,Symbol,Any,Any]
//
//    val protoDreqUUID = getUUID()
//    val protoDrspUUID = getUUID()
//
//    object MonadicDMsgs extends MsgTypes {
//
//      override def protoDreq : DReq =
//	MDGetRequest( $('protoDReq)( "yo!" ) )
//      override def protoDrsp : DRsp =
//	MDGetResponse( $('protoDRsp)( "oy!" ), Symbol( aLabel.toString ) )
//      override def protoJtsreq : JTSReq =
//	JustifiedRequest(
//	  protoDreqUUID,
//	  new URI( "agent", protoDreqUUID.toString, "/invitation", "" ),
//	  new URI( "agent", protoDreqUUID.toString, "/invitation", "" ),
//	  getUUID(),
//	  protoDreq,
//	  None
//	)
//      override def protoJtsrsp : JTSRsp =
//	JustifiedResponse(
//	  protoDreqUUID,
//	  new URI( "agent", protoDrspUUID.toString, "/invitation", "" ),
//	  new URI( "agent", protoDrspUUID.toString, "/invitation", "" ),
//	  getUUID(),
//	  protoDrsp,
//	  None
//	)
//      override def protoJtsreqorrsp : JTSReqOrRsp =
//	Left( protoJtsreq )
//    }
//
//    override def protoMsgs : MsgTypes = MonadicDMsgs
//  }

}
