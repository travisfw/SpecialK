// -*- mode: Scala;-*- 
// Filename:    AgentTermSpace.scala 
// Authors:     lgm                                                    
// Creation:    Thu Mar  3 12:37:15 2011 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.model.store

import com.biosimilarity.lift.model.store.xml._
import com.biosimilarity.lift.model.agent._
import com.biosimilarity.lift.model.msg._
import com.biosimilarity.lift.lib._
import com.biosimilarity.lift.lib.moniker._
import com.biosimilarity.lift.lib.extensions.StringExtensions._
//import com.biosimilarity.lift.lib.extensions.URIExtensions._

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
import java.util.UUID
import java.io.File
import java.io.FileInputStream
import java.io.OutputStreamWriter


trait AgentTermStoreScope[Namespace,Var,Tag,Value] 
extends PersistedTermStoreScope[Namespace,Var,Tag,Value]
with CnxnDTSMsgScope[Namespace,Var,Tag,Value]
with AgentCnxnTypeScope {  
  abstract class AgentMonadicGeneratorJunction(
    //override val name : URI,
    override val name : Moniker,
    //override val acquaintances : Seq[URI],
    override val acquaintances : Seq[Moniker],
    val cnxn : Option[acT.AgentCnxn],
    val cnxnPartition : HashMap[acT.AgentCnxn,AgentMonadicGeneratorJunction]
  ) extends PersistedMonadicGeneratorJunction(
    name,
    acquaintances
  ) with Blobify {            
    def makeSpace( cnxn : acT.AgentCnxn ) : AgentMonadicGeneratorJunction
    def ptnCnxnWrapperNamespace : String = "patternConnection"
    def embedCnxn(
      cnxn : acT.AgentCnxn,
      ptn : CnxnCtxtLabel[Namespace,Var,Tag] with Factual
    ) : Option[CnxnCtxtLabel[Namespace,Var,Tag] with Factual] = {
      for(
	ltns <- labelToNS;
	ttt <- textToTag;
	pm <- persistenceManifest;
	if ( pm.isInstanceOf[XMLDBManifest] )
      ) yield {
	val xmldbPm = pm.asInstanceOf[XMLDBManifest]
	val xmlifier = xmldbPm.xmlIfier
	
	val embeddedCnxn =
	  new CnxnCtxtBranch[Namespace,Var,Tag](
	    ltns( ptnCnxnWrapperNamespace ),
	    List(
	      xmlIfier.tolabeledBlob [Namespace,Var,Tag]( ltns, ttt )( cnxn ),
	      ptn
	    )
	  )
//	report(
//	  (
//	    ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
//	    + " embedding cnxn " + embeddedCnxn.toString
//	    + ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
//	  )
//	)
	embeddedCnxn
      }
    }

    def extractCnxn(
      ccl : CnxnCtxtLabel[Namespace,Var,Tag]
    ) : Option[( acT.AgentCnxn, CnxnCtxtLabel[Namespace,Var,Tag] with Factual )] = {
      ccl match {
	case CnxnCtxtBranch( ptnCnxnNS, cnxnBlob :: rs :: Nil ) => {
	  for(
	    pm <- persistenceManifest;
	    if ( pm.isInstanceOf[XMLDBManifest] )	      
	  ) yield {
//	    report(
//	      (
//		">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
//		+ " embedded cnxn " + ccl.toString
//		+ ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>"
//	      )
//	    )
	    val xmldbPm = pm.asInstanceOf[XMLDBManifest]
	    val xmlifier = xmldbPm.xmlIfier
	    val prod =
	      xmlifier.fromlabeledBlob( cnxnBlob ).getOrElse(
		throw new Exception( "must have a cnxn" )
	      )
	      ( prod.asInstanceOf[acT.AgentCnxn], rs )
	    }
	}
	case _ => {
	  None
	}
      }
    }

    def forward( cnxn : acT.AgentCnxn )(
      ask : dAT.AskNum,
      //hops : List[URI],
      hops : List[Moniker],
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    ) : Unit = {

      report(
	( this + " in forwardGet with hops: " + hops )
      )

      for(
	( uri, jsndr ) <- agentTwistedPairs
	if !hops.contains( uri )
      ) {
	report(
	  ( this + " forwarding to " + uri )
	)
	val smajatp : SMAJATwistedPair =
	  jsndr.asInstanceOf[SMAJATwistedPair]

	// BUGBUG -- LGM: fix typing so we don't have to cast
	for(
	  embeddedCnxn
	  <- embedCnxn(
	    cnxn,
	    path.asInstanceOf[CnxnCtxtLabel[Namespace,Var,Tag] with Factual]
	  )
	) {
	  smajatp.send(
	    ask match {
	      case dAT.AGetNum => {
		Msgs.MDGetRequest[Namespace,Var,Tag,Value](
		  embeddedCnxn
		).asInstanceOf[Msgs.DReq]
	      }
	      case dAT.AFetchNum => {
		Msgs.MDFetchRequest[Namespace,Var,Tag,Value](
		  embeddedCnxn
		).asInstanceOf[Msgs.DReq]
	      }
	      case dAT.ASubscribeNum => {
		Msgs.MDSubscribeRequest[Namespace,Var,Tag,Value](
		  embeddedCnxn
		).asInstanceOf[Msgs.DReq]
	      }
	    }
	  )
	}
      }
    }

    def mget( cnxn : acT.AgentCnxn )(
      persist : Option[PersistenceManifest],
      ask : dAT.AskNum,
      //hops : List[URI]
      hops : List[Moniker]
    )(
      channels : Map[mTT.GetRequest,mTT.Resource],
      registered : Map[mTT.GetRequest,List[RK]],
      consume : Boolean,
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
			  report( ">>>>> forwarding..." )
			  forward( cnxn )( ask, hops, path )
			  rk( oV )
			}
			case Some( pd ) => {
			  report(
			    "accessing db : " + pd.db
			  )
			  val xmlCollName =
			    collName.getOrElse(
			      storeUnitStr.getOrElse(
				bail()
			      )
			    )
			  
			  // Defensively check that db is actually available
			  
			  checkIfDBExists( xmlCollName, true ) match {
			    case true => {
			      val oQry = query( xmlCollName, path )
			      
			      oQry match {
				case None => {
				  report( ">>>>> forwarding..." )
				  forward( cnxn )( ask, hops, path )
				  rk( oV )
				}
				case Some( qry ) => {			      
				  report(
				    (
				      "querying db : " + pd.db
				      + " from coll " + xmlCollName
				      + " where " + qry
                                      , Severity.Trace
				    )
				  )
				  
				  val rslts = executeWithResults( qry )
				  
				  rslts match {				
				    case Nil => {	
				      report(
					(
					  "database "
					  + xmlCollName
					  + " had no matching resources."
					)
				      )
				      forward( cnxn )( ask, hops, path )
				      rk( oV )
				    }
				    case _ => {				  		  
				      report(
					(
					  "database "
					  + xmlCollName
					  + " had "
					  + rslts.length
					  + " matching resources."
					)
				      )
				      
				      for( rslt <- itergen[Elem]( rslts ) ) {
					report( "retrieved " + rslt.toString, Severity.Trace )
					val ersrc = asResource( path, rslt )
					
					if ( consume ) {
					  report( "removing from store " + rslt, Severity.Trace )
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
					report( "returning " + ersrc )
					rk( ersrc )
				      }			  				  
				    }
				  }
				}			    
			      }
			    }
			    case false => {
			      report( ">>>>> forwarding..." )
			      forward( cnxn )( ask, hops, path )
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

    def cnxnMatch(
      cnxn1 : acT.AgentCnxn,
      cnxn2 : acT.AgentCnxn
    ) : Boolean = {

      report(
	(
	  "matching " + cnxn1 + " with " + cnxn2 + "\n"
	  + "cnxn1.src = " + cnxn1.src
	  + " cnxn2.src = " + cnxn2.src + "\n"
	  + " src's "
	  + (
	    if ( cnxn1.src == cnxn2.src ) {
	      " match "
	    }
	    else {
	      " do not match "
	    }
	  )
	  + "\n"
	  + "cnxn1.trgt = " + cnxn1.trgt
	  + " cnxn2.trgt = " + cnxn2.trgt + "\n"
	  + " trgt's "
	  + (
	    if ( cnxn1.trgt == cnxn2.trgt ) {
	      " match "
	    }
	    else {
	       " do not match "
	     }
	  )
	  + "\n"
	  + "cnxn1.label = " + cnxn1.label
	  + " cnxn2.label = " + cnxn2.label + "\n"
	  + " label's "
	  + (
	    if ( cnxn1.label == cnxn2.label ) {
	      " match "
	    }
	    else {
	       " do not match "
	     }
	  )
	)
        , Severity.Trace
      )

      val rslt =
	(( cnxn1 == cnxn2 )
	 || (( cnxn1.src == cnxn2.src )
	     && ( cnxn1.trgt == cnxn2.trgt )
	     && ( cnxn1.label == cnxn2.label)));
	
      report( if ( rslt ) "matched" else "did not match", Severity.Trace )

      rslt
    }    

    def searchCnxnPartition(
      cnxn : acT.AgentCnxn
    ) : Option[AgentMonadicGeneratorJunction] = {
      // local recursion that stops just when the match is found
      def search(
	pairs : List[( acT.AgentCnxn, AgentMonadicGeneratorJunction )]
      ) : Option[( acT.AgentCnxn, AgentMonadicGeneratorJunction )] = {
	pairs match {
	  case ( cnxnKey, part ) :: rpairs => {
	    if ( cnxnMatch( cnxn, cnxnKey ) ) {
	      Some( ( cnxnKey, part ) )
	    }
	    else {
	      search( rpairs )
	    }
	  }
	  case Nil => None
	}
      }

      report(
	"Map failed searching map pairs : " + cnxnPartition.toList, Severity.Trace
      )
      
      for( ( cnxnKey, part ) <- search( cnxnPartition.toList ) ) 
	yield { part }
    }

    def getPartition(
      cnxn : acT.AgentCnxn
    ) : AgentMonadicGeneratorJunction = {
      cnxnPartition.get( cnxn ) match {
	// BUGBUG -- LGM : this is a workaround until we have
	// invitation and introduction protocols for PlatformAgents
	case None => {
	  searchCnxnPartition( cnxn ) match {
	    case None => {
	      report(
		(
		  "No matching space for "
		  + cnxn
		  + "\n"
		  + "Creating a new one"
		)
	      )
	      val npmgj = makeSpace( cnxn )
	      npmgj.agentTwistedPairs
	      cnxnPartition( cnxn ) = npmgj
	      npmgj
	    }
	    case Some( npmgj ) => {
	      report(
		"Found cnxn matching through search " + cnxn + "\n"
	      )
	      npmgj
	    }
	  }
	}
	case Some( npmgj ) => {
	  report(
	    "Found matching space for " + cnxn
	  )
	  npmgj
	}
      }
    }

    def getLocalPartition(
      cnxn : acT.AgentCnxn   // C( localProvider, l, remoteRequester )
    ) : AgentMonadicGeneratorJunction = {
      report(
	"Getting local partition using " + cnxn
      )
      getPartition( cnxn )   // C( localProvider, l, remoteRequester )
    }

    def getRemotePartition(
      cnxn : acT.AgentCnxn   // C( remoteRequester, l, localProvider )
    ) : AgentMonadicGeneratorJunction = {
      report(
	"Getting remote partition using " + cnxn
      )
//       val rvrsCnxn =         // C( localProvider, l, remoteRequester )
// 	acT.AgentCnxn( cnxn.trgt, cnxn.label, cnxn.src )
      
//       getLocalPartition( rvrsCnxn )
      // Since there is now a queue/partition we could only have
      // received this on the partition handling this queue; hence,
      // there is no further need for lookup
      this
    }

    def getPartitionActuals(
      cnxn : acT.AgentCnxn,
      partFn : acT.AgentCnxn => AgentMonadicGeneratorJunction
    ) = {
      val pmgj : AgentMonadicGeneratorJunction = partFn( cnxn )
      val perD = pmgj.persistenceManifest
      val xmlCollName = 
	perD match {
	  case None => None
	  case Some( pd ) => Some( pd.storeUnitStr( cnxn ) )
	}
      ( pmgj, perD, xmlCollName )
    }

    def getLocalPartitionActuals( cnxn : acT.AgentCnxn ) = {
      getPartitionActuals( cnxn, getLocalPartition )      
    }

    def getRemotePartitionActuals( cnxn : acT.AgentCnxn ) = {
      getPartitionActuals( cnxn, getRemotePartition ) 
    }

    def put( cnxn : acT.AgentCnxn )(
      ptn : mTT.GetRequest, rsrc : mTT.Resource
    ) = {
      report(
	"In cnxn-based put with cnxn " + cnxn
      )

      report(
	"Partitions : " + cnxnPartition.toList
        , Severity.Trace
      )

      val ( pmgj, perD, xmlCollName ) =
	getLocalPartitionActuals( cnxn )
      
      report(
	"Partitions : " + cnxnPartition.toList
        , Severity.Trace
      )

      report(
	"Storing " + ptn + " " + rsrc + " in partition " + pmgj
        , Severity.Trace
      )

      pmgj.mput( perD )(
	pmgj.theMeetingPlace, pmgj.theWaiters, false, xmlCollName
      )( ptn, rsrc )
    }
    
    def publish( cnxn : acT.AgentCnxn )(
      ptn : mTT.GetRequest, rsrc : mTT.Resource
    ) = {
      report(
	"In cnxn-based publish with cnxn " + cnxn
        , Severity.Trace
      )
      val ( pmgj, perD, xmlCollName ) =	getLocalPartitionActuals( cnxn )

      report(
	"Publishing " + rsrc + " on " + ptn + " in partition " + pmgj
        , Severity.Trace
      )
      
      pmgj.mput( perD )(
	pmgj.theChannels, pmgj.theSubscriptions, true, xmlCollName
      )( ptn, rsrc )
    }

    def remotePut( cnxn : acT.AgentCnxn )(
      ptn : mTT.GetRequest, rsrc : mTT.Resource
    ) = {
      report(
	"In cnxn-based put with cnxn " + cnxn
        , Severity.Trace
      )
      
      val ( pmgj, perD, xmlCollName ) =	getRemotePartitionActuals( cnxn )

      report(
	"Storing " + ptn + " " + rsrc + " in partition " + pmgj
        , Severity.Trace
      )

      pmgj.mput( perD )(
	pmgj.theMeetingPlace, pmgj.theWaiters, false, xmlCollName
      )( ptn, rsrc )
    }
    
    def remotePublish( cnxn : acT.AgentCnxn )(
      ptn : mTT.GetRequest, rsrc : mTT.Resource
    ) = {
      report(
	"In cnxn-based publish with cnxn " + cnxn
        , Severity.Trace
      )
      val ( pmgj, perD, xmlCollName ) =	getRemotePartitionActuals( cnxn )

      report(
	"Publishing " + rsrc + " on " + ptn + " in partition " + pmgj
        , Severity.Trace
      )
      
      pmgj.mput( perD )(
	pmgj.theChannels, pmgj.theSubscriptions, true, xmlCollName
      )( ptn, rsrc )
    }    

    //def get( hops : List[URI] )(
    def get( hops : List[Moniker] )(
      cnxn : acT.AgentCnxn
    )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {
      report(
	"In cnxn-based get with cnxn " + cnxn
        , Severity.Trace
      )

      report(
	"Partitions : " + cnxnPartition.toList
        , Severity.Trace
      )
      
      val ( pmgj, perD, xmlCollName ) =
	getLocalPartitionActuals( cnxn )

      report(
	"Partitions : " + cnxnPartition.toList
        , Severity.Trace
      )

      report(
	"Retrieving " + path + " from partition " + pmgj
        , Severity.Trace
      )

      pmgj.mget( cnxn )( perD, dAT.AGetNum, hops )(
	pmgj.theMeetingPlace, pmgj.theWaiters, true, xmlCollName
      )( path ).asInstanceOf[Generator[Option[mTT.Resource],Unit,Unit]]
    }    
    
    def get(
      cnxn : acT.AgentCnxn
    )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {        
      get( Nil )( cnxn )( path )    
    }

    def getValue(
      cnxn : acT.AgentCnxn
    )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    ) : Generator[Value,Unit,Unit] = 
      Generator {
	k : ( Value => Unit @suspendable ) =>
	  for(
	    orsrc <- get( cnxn )( path )
	    //rsrc <- orsrc
	    //gv <- getGV( rsrc )
	  ) {
	    orsrc match {
	      case Some( rsrc ) => {
		getGV( rsrc ) match {
		  case Some( gv ) => k( gv )
		  case None =>
		}
	      }
	      case None => 
	    };
	  }
      }

    //def remoteGet( hops : List[URI] )(
    def remoteGet( hops : List[Moniker] )(
      cnxn : acT.AgentCnxn
    )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {
      report(
	"In cnxn-based get with cnxn " + cnxn
        , Severity.Trace
      )

      val ( pmgj, perD, xmlCollName ) =	getRemotePartitionActuals( cnxn )

      report(
	"Retrieving " + path + " from partition " + pmgj
        , Severity.Trace
      )

      pmgj.mget( cnxn )( perD, dAT.AGetNum, hops )(
	pmgj.theMeetingPlace, pmgj.theWaiters, true, xmlCollName
      )( path ).asInstanceOf[Generator[Option[mTT.Resource],Unit,Unit]]
    }

    //def fetch( hops : List[URI] )(
    def fetch( hops : List[Moniker] )(
      cnxn : acT.AgentCnxn
    )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {   
      report(
	"In cnxn-based fetch with cnxn " + cnxn
        , Severity.Trace
      )
      
      val ( pmgj, perD, xmlCollName ) =	getLocalPartitionActuals( cnxn )

      report(
	"Retrieving " + path + " from partition " + pmgj
        , Severity.Trace
      )

      pmgj.mget( cnxn )( perD, dAT.AFetchNum, hops )(
	pmgj.theMeetingPlace, pmgj.theWaiters, false, xmlCollName
      )( path ).asInstanceOf[Generator[Option[mTT.Resource],Unit,Unit]]
    }

    def fetch(
      cnxn : acT.AgentCnxn
    )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {        
      fetch( Nil )( cnxn )( path )    
    }

    //def remoteFetch( hops : List[URI] )(
    def remoteFetch( hops : List[Moniker] )(
      cnxn : acT.AgentCnxn
    )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {   
      report(
	"In cnxn-based fetch with cnxn " + cnxn
        , Severity.Trace
      )
      
      val ( pmgj, perD, xmlCollName ) =	getRemotePartitionActuals( cnxn )

      report(
	"Retrieving " + path + " from partition " + pmgj
        , Severity.Trace
      )

      pmgj.mget( cnxn )( perD, dAT.AFetchNum, hops )(
	pmgj.theMeetingPlace, pmgj.theWaiters, false, xmlCollName
      )( path ).asInstanceOf[Generator[Option[mTT.Resource],Unit,Unit]]
    }

    //def subscribe( hops : List[URI] )(
    def subscribe( hops : List[Moniker] )(
      cnxn : acT.AgentCnxn
    )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {    
      report(
	"In cnxn-based subscribe with cnxn " + cnxn
        , Severity.Trace
      )
      
      val ( pmgj, perD, xmlCollName ) =	getLocalPartitionActuals( cnxn )

      report(
	"Retrieving " + path + " from partition " + pmgj
        , Severity.Trace
      )

      pmgj.mget( cnxn )( perD, dAT.ASubscribeNum, hops )(
	pmgj.theChannels, pmgj.theSubscriptions, true, xmlCollName
      )( path ).asInstanceOf[Generator[Option[mTT.Resource],Unit,Unit]]
    }

    def subscribe(
      cnxn : acT.AgentCnxn
    )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {        
      subscribe( Nil )( cnxn )( path )    
    }

    //def remoteSubscribe( hops : List[URI] )(
    def remoteSubscribe( hops : List[Moniker] )(
      cnxn : acT.AgentCnxn
    )(
      path : CnxnCtxtLabel[Namespace,Var,Tag]
    )
    : Generator[Option[mTT.Resource],Unit,Unit] = {    
      report(
	"In cnxn-based subscribe with cnxn " + cnxn
        , Severity.Trace
      )
      
      val ( pmgj, perD, xmlCollName ) =	getRemotePartitionActuals( cnxn )

      report(
	"Retrieving " + path + " from partition " + pmgj
        , Severity.Trace
      )

      pmgj.mget( cnxn )( perD, dAT.ASubscribeNum, hops )(
	pmgj.theChannels, pmgj.theSubscriptions, true, xmlCollName
      )( path ).asInstanceOf[Generator[Option[mTT.Resource],Unit,Unit]]
    }

    def store(
      cnxn : acT.AgentCnxn
    )(
      ptn : mTT.GetRequest,
      rsrc : mTT.Resource
    ) : Unit = {
      // BUGBUG -- LGM this should store at all patterns
      // It looks as if putPlaces is not doing the full calculation
      report(
	"In cnxn-based store with cnxn " + cnxn
        , Severity.Trace
      )

      val pmgj = getLocalPartition( cnxn )
      
      report(
	"In cnxn-based store with partition " + pmgj
        , Severity.Trace
      )
      
      for( pd <- pmgj.persistenceManifest ) {
	spawn {
	  val rcrd = pd.asStoreRecord( ptn, rsrc )
	  report(
	    (
	      "storing to db : " + pd.db
	      + " pair : " + rcrd
	      + " in coll : " + pd.storeUnitStr( cnxn )
	    )
	  )
	  store( pd.storeUnitStr( cnxn ) )( rcrd )
	}
      }
    }    

    def delete(
      cnxn : acT.AgentCnxn
    )(
      path : CnxnCtxtLabel[Namespace,Var,String]
    )
    : Unit = {
      tweet(
	"In cnxn-based delete with cnxn " + cnxn
      )

      val pmgj = getLocalPartition( cnxn )

      tweet(
	"In cnxn-based delete with partition " + pmgj
      )

      for( pd <- pmgj.persistenceManifest ) {
	spawn {
	  tweet(
	    (
	      "deleting from db : " + pd.db
	      + " key : " + path.toString
	      + " in coll : " + pd.storeUnitStr( cnxn )
	    )
	  )
	  delete(pd.storeUnitStr( cnxn ), path)
	}
      }
    }

    override def handleRequest( dreq : Msgs.JTSReq ) : Unit = {
      val JustifiedRequest( 
	msgId, mtrgt, msrc, lbl, body, _
      ) = dreq

      report( this + "handling : " + dreq, Severity.Trace )

      body match {
	case dgreq@Msgs.MDGetRequest( path ) => {	  
	  report(
	    ( this + "getting locally for location : " + path )
	  )
	  for( ( cnxn, npath ) <- extractCnxn( path ) ) {
	    reset {
	      for( v <- remoteGet( List( msrc ) )( cnxn )( npath ) ) {
		report(
		  (
		    this 
		    + " returning from local get for location : "
		    + path
		    + "\nwith value : " + v
                    , Severity.Trace
		  )
		)
		handleValue( dgreq, v, msrc )
	      }
	    }
	  }
	}
	
	case dfreq@Msgs.MDFetchRequest( path ) => {
	  report(
	    ( this + "fetching locally for location : " + path )
	  )
	  for( ( cnxn, npath ) <- extractCnxn( path ) ) {
	    reset {
	      for( v <- remoteFetch( List( msrc ) )( cnxn )( npath ) ) {
		report(
		  (
		    this 
		    + " returning from local fetch for location : "
		    + path
		    + "\nwith value : " + v
                    , Severity.Trace
		  )
		)
		handleValue( dfreq, v, msrc )
	      }
	    }
	  }
	}

	case dsreq@Msgs.MDSubscribeRequest( path ) => {
	  report(
	    ( this + "fetching locally for location : " + path )
	  )
	  for( ( cnxn, npath ) <- extractCnxn( path ) ) {
	    reset {
	      for( v <- remoteSubscribe( List( msrc ) )( cnxn )( npath ) ) {
		report(
		  (
		    this 
		    + " returning from local subscribe for location : "
		    + path
		    + "\nwith value : " + v
                    , Severity.Trace
		  )
		)
		handleValue( dsreq, v, msrc )
	      }
	    }
	  }
	}
	
	case dpreq@Msgs.MDPutRequest( path, value ) => {	
	  reset { put( path, mTT.Ground( value ) ) }
	  for( atp <- agentTwistedPairs.get( msrc ) ) {
	    sendRsp( atp, dpreq, None )
	  }
	}
	case dpbreq@Msgs.MDPublishRequest( path, value ) => {	
	  reset { publish( path, mTT.Ground( value ) ) }
	  for( atp <- agentTwistedPairs.get( msrc ) ) {
	    sendRsp( atp, dpbreq, None )
	  }
	}
      }
    }
    
    override def handleResponse( drsp : Msgs.JTSRsp ) : Unit = {      
      val JustifiedResponse( 
	  msgId, mtrgt, msrc, lbl, body, _
      ) = drsp

      body match {
	case Msgs.MDGetResponse( path, value ) => {
	  for( ( cnxn, npath ) <- extractCnxn( path ) ) {
	    reset {
	      remotePut( cnxn )( npath, mTT.Ground( value ) )
	    }
	  }
	}
	case Msgs.MDFetchResponse( path, value ) => {
	  for( ( cnxn, npath ) <- extractCnxn( path ) ) {
	    reset {
	      remotePut( cnxn )( npath, mTT.Ground( value ) )
	    }
	  }
	}
	case Msgs.MDSubscribeResponse( path, value ) => {
	  for( ( cnxn, npath ) <- extractCnxn( path ) ) {
	    reset {
	      remotePublish( cnxn )( npath, mTT.Ground( value ) )
	    }
	  }
	}
	case dput : Msgs.MDPutResponse[Namespace,Var,Tag,Value] => {	
	}
	case dpub : Msgs.MDPublishResponse[Namespace,Var,Tag,Value] => {	
	}
	case _ => {
	  report(
	    (
	      this 
	      + " handling unexpected message : " + body
              , Severity.Error
	    )
	  )
	}
      }
    }
  }
}

/* ------------------------------------------------------------------
 * Mostly self-contained object to support unit testing
 * ------------------------------------------------------------------ */ 

object AgentTS
 extends AgentTermStoreScope[String,String,String,String]
  with UUIDOps {
    import SpecialKURIDefaults._
    import CnxnLeafAndBranch._
    import identityConversions._
    
    type ACTypes = AgentCnxnTypes
    object TheACT extends ACTypes
    override def protoAgentCnxnTypes : ACTypes = TheACT
    
    type MTTypes = MonadicTermTypes[String,String,String,String]
    object TheMTT extends MTTypes
    override def protoTermTypes : MTTypes = TheMTT
    
    type DATypes = DistributedAskTypes
    object TheDAT extends DATypes
    override def protoAskTypes : DATypes = TheDAT
    
    type MsgTypes = DTSMSH[String,String,String,String]   
    type CnxnMsgTypes = CnxnDTSMSH[String,String,String,String]
    
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

    object MonadicCnxnDMsgs extends CnxnMsgTypes {      
      type ACTypes = AgentCnxnTypes
      type MsgTypes =
      DTSMSH[String,String,String,String]   

      override def protoMsgs : MsgTypes = MonadicDMsgs    
      override def protoAgentCnxnTypes : ACTypes = TheACT

      override def protoCnxnDreq : CnxnDReq =
	CnxnMDGetRequest( acT.protoAgentCnxn, aLabel )
      override def protoCnxnDrsp : CnxnDRsp =
	CnxnMDGetResponse(
	  acT.protoAgentCnxn,
	  aLabel,
	  getUUID().toString
	)
      override def protoCnxnJtsreq : CnxnJTSReq =
	JustifiedRequest(
	  protoDreqUUID,
	  new URI( "agent", protoDreqUUID.toString, "/invitation", "" ),
	  new URI( "agent", protoDreqUUID.toString, "/invitation", "" ),
	  getUUID(),
	  protoCnxnDreq,
	  None
	)
      override def protoCnxnJtsrsp : CnxnJTSRsp = 
	JustifiedResponse(
	  protoDreqUUID,
	  new URI( "agent", protoDrspUUID.toString, "/invitation", "" ),
	  new URI( "agent", protoDrspUUID.toString, "/invitation", "" ),
	  getUUID(),
	  protoCnxnDrsp,
	  None
	)
      override def protoCnxnJtsreqorrsp : CnxnJTSReqOrRsp =
	Left( protoCnxnJtsreq )
    }

    override def protoCnxnMsgs : CnxnMsgTypes = MonadicCnxnDMsgs
    
    class PartitionedStringMGJ(
      //override val name : URI,
      override val name : Moniker,
      //override val acquaintances : Seq[URI],
      override val acquaintances : Seq[Moniker],
      override val cnxn : Option[acT.AgentCnxn]
    ) extends AgentMonadicGeneratorJunction(
      name, acquaintances, cnxn,
      new HashMap[acT.AgentCnxn,AgentMonadicGeneratorJunction]()
    ) {
      class StringXMLDBManifest(
	override val labelToNS : Option[String => String],
	override val textToVar : Option[String => String],
	override val textToTag : Option[String => String]
      )
      extends XMLDBManifest( database ) {
	override def storeUnitStr : String = {
	  throw new Exception( "use Cnxn-based interface instead" )
	}
	override def storeUnitStr[Src,Label,Trgt](
	  cnxn : Cnxn[Src,Label,Trgt]
	) : String = {     
	  cnxn match {
	    case agentCnxn : acT.AgentCnxn =>
	      agentCnxn.src.getHost + agentCnxn.trgt.getHost
	    case _ =>
	      throw new Exception( "unexpected cnxn type" )
	  }
	}    

	def kvNameSpace : String = "record"

	override def asCacheValue(
	  ltns : String => String,
	  ttv : String => String,
	  value : Elem
	) : Option[String] = {
	  report(
	    "Shouldn't be here!"
            , Severity.Error
	  )
	  None
	}

	override def asStoreValue(
	  rsrc : mTT.Resource
	) : CnxnCtxtLeaf[String,String,String] with Factual = {
	  valueStorageType match {
	    case "CnxnCtxtLabel" => {
	      report(
		"warning: CnxnCtxtLabel method is using XStream"
                , Severity.Trace
	      )

	      val blob = toXQSafeJSONBlob( rsrc )

	      new CnxnCtxtLeaf[String,String,String](
		Left[String,String](
		  blob
		)
	      )
	    }
	    case "XStream" => {
	      report(
		"using XStream method"
                , Severity.Trace
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

	override def asCacheValue(
	  ccl : CnxnCtxtLabel[String,String,String]
	) : String = {
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
      
      def makeSpace( cnxn : acT.AgentCnxn ) = {
	val symmIdStr = 
	  cnxn.symmetricIdentityString
	report(
	  (
	    "Symmetric cnxn identity is " 
	    + symmIdStr
	  )
	)		  
		
	val nacqs = 
	  (
	    for( acq <- acquaintances )
	      yield {
		acq.withPath(acq.getPath + "/" + symmIdStr)
	      }
	  )
	  
	val nname = name.withPath(name.getPath + "/" + symmIdStr)

	new PartitionedStringMGJ( nname, nacqs, Some( cnxn ) )	
      }
      
      var _persistenceManifest : Option[PersistenceManifest] = None
      def persistenceManifest : Option[PersistenceManifest] = {
	_persistenceManifest match {
	  case None => {
	    val sid = Some( ( s : String ) => s )
	    val pm =
	      Some(
		new StringXMLDBManifest( sid, sid, sid )
	      )
	    _persistenceManifest = pm
	    pm
	  }
	  case Some( _ ) => _persistenceManifest
	}
      }
      
      import scala.collection.immutable.IndexedSeq
            
    }

    def ptToPt( a : URI, b : URI )  =
      new PartitionedStringMGJ( a, List( b ), None )

    def ptToPt( a : String, b : String )  =
      new PartitionedStringMGJ( a.toURI, List( b.toURI ), None )

    def ptToPts( a : URI, acquaintances : List[URI] )  = {
      new PartitionedStringMGJ( a, acquaintances.map( toMoniker ), None )
    }

    def ptToPts( a : String, acquaintances : List[String] )  = {
      new PartitionedStringMGJ( a.toURI, acquaintances.map( toMoniker ), None )
    }

    def ptToMany( a : String, acquaintances : List[String] )  = {
      //new PartitionedStringMGJ( a.toURI, acquaintances.map(_.toURI), None )
      new PartitionedStringMGJ( a.toURI, List("".toURI), None )
    }    


    object PingPong {      
      implicit def toPattern(
	  s : String
      ) : CnxnCtxtLabel[String,String,String] with Factual =
	fromCaseClassInstanceString(
	  s
	).getOrElse(
	  null
	).asInstanceOf[CnxnCtxtLabel[String,String,String] with Factual]

      implicit def toValue(
	s : String
      ) : mTT.Resource = mTT.Ground( s )	             

      def pingPong( a : String,	b : String, rounds : Int, parity : Boolean )
      = {	
	// make a kvdb instance
	val mySpace = ptToPt( a, b )

	// turn down reporting
        //todo: add this back in after log changes
//	mySpace.setLoggingLevel(
//	  Luddite(
//	    mySpace.journalIDVender.getUUID
//	  )
//	)
	
	// make a connection
	val cnxn = new acT.AgentCnxn(
	  "ping".toURI, "volley", "pong".toURI
	)

	// local recursive protocol driver
	def pingPongRec( whereToPing : String ) : Unit = {
	  reset {
	    println( "waiting for data on pattern : " + whereToPing )
	    for(
	      pong <- mySpace.getValue( cnxn )( 
		whereToPing
	      )
	    ) {
	      pong match {
		case "stop" => {
		  println( "stopping protocol." )
		}
		case _ => {
		  println( "received : " + pong )
		  val round = pong.toInt
		  if ( round > 1 ) {
		    val nextPingSpot = 
		      "ping( " + ( round - 1 ).toString + " )"
		    val nextNextPingSpot = 
		      "ping( " + ( round - 2 ).toString + " )"

		    println(
		      (
			"putting : " + ( round - 1).toString
			+ " on " + nextPingSpot
		      )
		    )
		    mySpace.put( cnxn )( nextPingSpot, ( round - 1 ).toString )
		    pingPongRec( nextNextPingSpot )
		  }
		  else {
		    val nextPingSpot = 
		      "ping( " + ( round - 1 ).toString + " )"

		    mySpace.put( cnxn )( nextPingSpot, "stop" )
		  }
		}
	      }
	    }
	  }
	}

	// initial conditions
	val initialPingChannel = 
	  "ping( " + rounds.toString + " )"
	val nextPingChannel = 
	  "ping( " + ( rounds - 1 ).toString + " )"
	
	println( "initiating protocol." )

	// begin the protocol
	if ( parity ) {
	  println(
	    (
	      "putting : " + rounds.toString
	      + " on " + initialPingChannel
	    )
	  )
	  
	  reset {
	    mySpace.put( cnxn )(
	      initialPingChannel,
	      rounds.toString
	    )
	  }

	  pingPongRec( nextPingChannel )
	}	
	else {
	  pingPongRec( initialPingChannel )
	}	
	
      }
    }
  }

object StdAgentMonadicTS
 extends AgentTermStoreScope[Symbol,Symbol,Any,Any] 
  with UUIDOps {
    import SpecialKURIDefaults._
    import CnxnLeafAndBranch._
    import CCLDSL._
    import identityConversions._

    type ACTypes = AgentCnxnTypes
    object TheACT extends ACTypes
    override def protoAgentCnxnTypes : ACTypes = TheACT
    
    type MTTypes = MonadicTermTypes[Symbol,Symbol,Any,Any]
    object TheMTT extends MTTypes
    override def protoTermTypes : MTTypes = TheMTT

    type DATypes = DistributedAskTypes
    object TheDAT extends DATypes
    override def protoAskTypes : DATypes = TheDAT

    import scala.collection.immutable.IndexedSeq
        
    type MsgTypes = DTSMSH[Symbol,Symbol,Any,Any]  
    type CnxnMsgTypes = CnxnDTSMSH[Symbol,Symbol,Any,Any]
    
    val protoDreqUUID = getUUID()
    val protoDrspUUID = getUUID()    
    
    object MonadicDMsgs extends MsgTypes {
      
      override def protoDreq : DReq = 
	MDGetRequest( $('protoDReq)( "yo!" ) )
      override def protoDrsp : DRsp =
	MDGetResponse( $('protoDRsp)( "oy!" ), Symbol( aLabel.toString ) )
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

    object MonadicCnxnDMsgs extends CnxnMsgTypes {      
      type ACTypes = AgentCnxnTypes
      type MsgTypes =
      DTSMSH[Symbol,Symbol,Any,Any]   

      override def protoMsgs : MsgTypes = MonadicDMsgs    
      override def protoAgentCnxnTypes : ACTypes = TheACT

      override def protoCnxnDreq : CnxnDReq =
	CnxnMDGetRequest(
	  acT.protoAgentCnxn,
	  $('protoDReq)( "yo!" )
	)
      override def protoCnxnDrsp : CnxnDRsp =
	CnxnMDGetResponse(
	  acT.protoAgentCnxn,
	  $('protoDRsp)( "oy!" ),
	  Symbol( aLabel.toString )
	)
      override def protoCnxnJtsreq : CnxnJTSReq =
	JustifiedRequest(
	  protoDreqUUID,
	  new URI( "agent", protoDreqUUID.toString, "/invitation", "" ),
	  new URI( "agent", protoDreqUUID.toString, "/invitation", "" ),
	  getUUID(),
	  protoCnxnDreq,
	  None
	)
      override def protoCnxnJtsrsp : CnxnJTSRsp = 
	JustifiedResponse(
	  protoDreqUUID,
	  new URI( "agent", protoDrspUUID.toString, "/invitation", "" ),
	  new URI( "agent", protoDrspUUID.toString, "/invitation", "" ),
	  getUUID(),
	  protoCnxnDrsp,
	  None
	)
      override def protoCnxnJtsreqorrsp : CnxnJTSReqOrRsp =
	Left( protoCnxnJtsreq )
    }

    override def protoCnxnMsgs : CnxnMsgTypes = MonadicCnxnDMsgs
    
    class PartitionedStdMGJ(
      //override val name : URI,
      override val name : Moniker,
      //override val acquaintances : Seq[URI],
      override val acquaintances : Seq[Moniker],
      override val cnxn : Option[acT.AgentCnxn]
    ) extends AgentMonadicGeneratorJunction(
      name, acquaintances, cnxn,
      new HashMap[acT.AgentCnxn,AgentMonadicGeneratorJunction]()
    ) {
      class StringXMLDBManifest(
	override val labelToNS : Option[String => Symbol],
	override val textToVar : Option[String => Symbol],
	override val textToTag : Option[String => Any]        
      )
      extends XMLDBManifest( database ) {
	override def storeUnitStr : String = {
	  throw new Exception( "use Cnxn-based interface instead" )
	}
	override def storeUnitStr[Src,Label,Trgt](
	  cnxn : Cnxn[Src,Label,Trgt]
	) : String = {     
	  cnxn match {
	    case agentCnxn : acT.AgentCnxn =>
	      agentCnxn.src.getHost + agentCnxn.trgt.getHost
	    case _ =>
	      throw new Exception( "unexpected cnxn type" )
	  }
	}	

	def kvNameSpace : Symbol = 'record

	// BUGBUG -- LGM: Evidence of a problem with this factorization
	override def asCacheValue(
	  ltns : String => Symbol,
	  ttv : String => Symbol,
	  value : Elem
	) : Option[String] = {
	  report(
	    "Shouldn't be here!"
            , Severity.Error
	  )
	  None
	}

	override def asStoreValue(
	  rsrc : mTT.Resource
	) : CnxnCtxtLeaf[Symbol,Symbol,String] with Factual = {
	  valueStorageType match {
	    case "CnxnCtxtLabel" => {
	      report(
		"warning: CnxnCtxtLabel method is using XStream"
                , Severity.Trace
	      )
	      
	      val blob = toXQSafeJSONBlob( rsrc )

	      new CnxnCtxtLeaf[Symbol,Symbol,String](
		Left[String,Symbol](
		  blob
		)
	      )
	    }
	    case "XStream" => {
	      report(
		"using XStream method"
                , Severity.Trace
	      )

	      val blob = toXQSafeJSONBlob( rsrc )
	      //asXML( rsrc )

	      new CnxnCtxtLeaf[Symbol,Symbol,String](
		Left[String,Symbol]( blob )
	      )
	    }
	    case _ => {
	      throw new Exception( "unexpected value storage type" )
	    }
	  }	  
	}

	def asCacheValue(
	  ccl : CnxnCtxtLabel[Symbol,Symbol,String]
	) : String = {
	  report(
	    "converting to cache value"
            , Severity.Trace
	  )
	  //asPatternString( ccl )
	  ccl match {
	    case CnxnCtxtBranch(
	      storeType,
	      CnxnCtxtLeaf( Left( rv ) ) :: Nil
	    ) => {
	      def extractValue( rv : String ) : String = {
		val unBlob = fromXQSafeJSONBlob( rv )

		unBlob match {
		  case rsrc : mTT.Resource => {
		    (getGV( rsrc ).getOrElse( "" ) + "")
		  }
		}
	      }

	      (storeType + "") match {
		case "String" => {
		  extractValue( rv )
		}
		case "'String" => {
		  extractValue( rv )
		}
	      }	      
	    }
	    case _ => {
	      asPatternString(
		ccl.asInstanceOf[CnxnCtxtLabel[Symbol,Symbol,Any]]
	      )
	    }
	  }
	}
      
      }

      def makeSpace( cnxn : acT.AgentCnxn ) = {
	val symmIdStr = 
	  cnxn.symmetricIdentityString

	report(
	  (
	    "Symmetric cnxn identity is " 
	    + symmIdStr
	  )
	)
		  		
	val nacqs = 
	  (
	    for( acq <- acquaintances )
	      yield {
                acq.withPath(acq.getPath + "/" + symmIdStr)
	      }
	  );

        val nname = name.withPath(name.getPath + "/" + symmIdStr)

	new PartitionedStdMGJ( nname, nacqs, Some( cnxn ) )
      }

      var _persistenceManifest : Option[PersistenceManifest] = None
      def persistenceManifest : Option[PersistenceManifest] = {
	_persistenceManifest match {
	  case None => {
	    val sid = Some( ( s : String ) => s )
	    val sym = Some( ( s : String ) => Symbol( s ) )
	    val pm =
	      Some(
		new StringXMLDBManifest( sym, sym, sid )
	      )
	    _persistenceManifest = pm
	    pm
	  }
	  case Some( _ ) => _persistenceManifest
	}
      }
    }
    
    def ptToPt( a : String, b : String )  = {
      new PartitionedStdMGJ( a, List( b ), None )
    }

    def loopBack() = {
      ptToPt( "localhost", "localhost" )
    }    
  }
