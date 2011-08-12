// -*- mode: Scala;-*- 
// Filename:    PersistedMonadicTermStoreTests.scala 
// Authors:     lgm                                                    
// Creation:    Tue Apr  5 20:08:45 2011 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.test.lib

import org.specs._
import org.specs.runner.JUnit3
import org.specs.runner.ConsoleRunner

import com.biosimilarity.lift.model.store._
import com.biosimilarity.lift.model.store.xml._
import com.biosimilarity.lift.model.store.xml.datasets._
import com.biosimilarity.lift.model.agent._
import com.biosimilarity.lift.model.msg._
import com.biosimilarity.lift.lib._
import com.biosimilarity.lift.lib.usage._

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


class RabbitMQConnectivityTest
extends JUnit3(RabbitMQConnectivityTestSpecs)

object RabbitMQConnectivityTestSpecsRunner
extends ConsoleRunner(RabbitMQConnectivityTestSpecs)


object RabbitMQConnectivityTestSpecs extends Specification {
  import AMQPDefaults._
  import MonadicAMQPUnitTest._
  "basic send / receive using monadic dispatching" should {
    "send and receive values over rabbitmq queue" in {
      var msgCount = 0
      val sma1 = SMJATwistedPair[Msg]( "localhost", "localhost" )
      val exchangeName = UUID.randomUUID().toString
      sma1.jsonSender( exchangeName )
      sma1.jsonDispatcher(
	exchangeName,
	( x ) => {
	  println( "received : " + x )
	  msgCount += 1
	}
      )
      val msgs = msgStrm.take( 100 )

      for( i <- 0 to 9 ) { sma1.send( msgs( i ) ) }
      
      while ( msgCount < 10 ) {
	Thread.sleep( 100 )
      }

      msgCount must be >= 10
    }
  }
}
