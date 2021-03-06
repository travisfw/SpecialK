// -*- mode: Scala;-*- 
// Filename:    PersistedMonadicTermStoreTests.scala 
// Authors:     lgm                                                    
// Creation:    Tue Apr  5 20:08:45 2011 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.test.store

import org.specs._
import org.specs.runner.JUnit4
import org.specs.runner.ConsoleRunner

import com.biosimilarity.lift.model.store._
import com.biosimilarity.lift.model.store.usage._
import com.biosimilarity.lift.model.store.xml._
import com.biosimilarity.lift.model.store.xml.datasets._
import com.biosimilarity.lift.model.agent._
import com.biosimilarity.lift.model.msg._
import com.biosimilarity.lift.lib._

import scala.concurrent.{Channel => Chan, _}
import scala.concurrent.cpsops._
import scala.util.continuations._ 
import scala.xml._

import org.prolog4j._

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


class PersistedMonadicTermStoreTest
extends JUnit4(PersistedMonadicTermStoreTestSpecs)

object PersistedMonadicTermStoreTestSpecsRunner
extends ConsoleRunner(PersistedMonadicTermStoreTestSpecs)


object PersistedMonadicTermStoreTestSpecs extends Specification {
  import PersistedMonadicTS._

  val dbSuffixes =
    List( "BasicFetch", "BasicCursorFetch", "BasicCursorGet" );

  def dbSuffix( i : Int ) = dbSuffixes( i - 1 )
  def dbFullName( i : Int ) = "GraphFour" + dbSuffix( i )

  val oge = BX.outerGraphExprCCL
      
  "basic fetch" should {
    BX.loadDataSetsClientSession( dbSuffix( 1 ) )
//    BX.reportGraphsClientSession
    val pimgJunq = ptToPt( dbFullName( 1 ), "localhost", "localhost" )
    val atps = pimgJunq.agentTwistedPairs	    

    var eVal = ""
    var sleepCount = 0

    "Fetch values from the GraphFour DB" in {
      println( "*****************************************" )
      println( "loading datasets for " + dbFullName( 1 ) )
      println( "*****************************************" )

      println( "*****************************************" )
      println( "query " + oge )
      println( "*****************************************" )
    
      reset {
	for( e <- pimgJunq.fetch( oge ) )
	  {
	    println( "received: " + e )
	    e match {
	      case Some(
		mTT.RBound(
		  Some( mTT.Ground( graphSpec ) ),
		  None
		)
	      ) => {
		eVal = e.toString
	      }
	      case _ => {
		throw new Exception(
		  "received unexpect value from	test " + e
		)
	      }
	    }
	  }
      }

      while ( eVal == "" && sleepCount < 25) {
        sleepCount += 1
	Thread.sleep( 100 )
      }

      eVal.length must be >= 0
      eVal.indexOf("Connected") must be >= 0
    }
    
  }
//
//  "basic cursor get" should {
//    BX.loadDataSetsClientSession( dbSuffix( 2 ) )
////    BX.reportGraphsClientSession
//    val pimgJunq = ptToPt( dbFullName( 2 ), "localhost", "localhost" )
//    val atps = pimgJunq.agentTwistedPairs
//
//    var eVal = ""
//    var sleepCount = 0
//
//    "Fetch values from the GraphFour DB by Cursor" in {
//      println( "*****************************************" )
//      println( "loading datasets for " + dbFullName( 2 ) )
//      println( "*****************************************" )
//
//      println( "*****************************************" )
//      println( "query " + oge )
//      println( "*****************************************" )
//
//      reset {
//	for( e <- pimgJunq.fetch( true )( oge ) )
//	  {
//	    println( "received: " + e )
//	    e match {
//	      case Some(
//		mTT.RBound(
//		  Some( mTT.Cursor( graphSpec ) ),
//		  None
//		)
//	      ) => {
//		for ( v <- graphSpec)
//                {
//                  eVal = eVal + v.toString
////                  eVal = "good"
//               }
//                println( "parsed cursor: " + eVal )
//	      }
//	      case _ => {
//		throw new Exception(
//		  "received unexpected value from test " + e
//		)
//	      }
//	    }
//	  }
//      }
//
//      while ( eVal == ""  && sleepCount < 25) {
//        sleepCount += 1
//	Thread.sleep( 100 )
//      }
//
//      eVal.length must be >= 0
//      eVal.indexOf("Connected") must be >= 0
//    }
//  }
//
//  "basic cursor get" should {
//    BX.loadDataSetsClientSession( dbSuffix( 3 ) )
////    BX.reportGraphsClientSession
//    val pimgJunq = ptToPt( dbFullName( 3 ), "localhost", "localhost" )
//    val atps = pimgJunq.agentTwistedPairs
//
//    var eVal = ""
//    var sleepCount = 0
//
//    "Get values from the GraphFour DB by Cursor" in {
//      println( "*****************************************" )
//      println( "loading datasets for " + dbFullName( 3 ) )
//      println( "*****************************************" )
//
//      println( "*****************************************" )
//      println( "query " + oge )
//      println( "*****************************************" )
//
//      reset {
//	for( e <- pimgJunq.get( true )( oge ) )
//	  {
//	    println( "received: " + e )
//	    e match {
//	      case Some(
//		mTT.RBound(
//		  Some( mTT.Cursor( graphSpec ) ),
//		  None
//		)
//	      ) => {
//		for ( v <- graphSpec)
//                {
//                  eVal = eVal + v.toString
////                  eVal = "good"
//                }
//                println( "parsed cursor: " + eVal )
//	      }
//	      case _ => {
//		throw new Exception(
//		  "received unexpected value from test " + e
//		)
//	      }
//	    }
//	  }
//      }
//
//      while ( eVal == ""  && sleepCount < 25) {
//        sleepCount += 1
//	Thread.sleep( 100 )
//      }
//
//      eVal.length must be >= 0
//      eVal.indexOf("Connected") must be >= 0
//    }
//  }
}
