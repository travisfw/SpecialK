// -*- mode: Scala;-*- 
// Filename:    AMQPMnd.scala 
// Authors:     lgm                                                    
// Creation:    Tue Jul 12 01:50:41 2011 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.lib

import com.biosimilarity.lift.lib.moniker._
import com.biosimilarity.lift.lib.monad._

import net.liftweb.amqp._

import scala.util.continuations._

import scala.concurrent.{Channel => Chan, _}
import scala.concurrent.cpsops._

import _root_.com.rabbitmq.client.{ Channel => RabbitChan, _}
import _root_.scala.actors.Actor

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver

import java.net.URI
import _root_.java.io.ObjectInputStream
import _root_.java.io.ObjectOutputStream
import _root_.java.io.ByteArrayInputStream
import _root_.java.io.ByteArrayOutputStream
import _root_.java.util.Timer
import _root_.java.util.TimerTask

case class AMQPScope[T](
  factory : ConnectionFactory,
  host : String,
  port : Int
) extends MonadicDispatcherScope[T] {
  self =>
  override type MDS[A] = AMQPQueueM[A]
  override def protoMDS[A] = new AMQPQueueM[A]( "", "" )

  case class AMQPQueue[A](    
    exchange : String,
    routingKey : String,
    dispatcher : theMDS.Generator[A,Unit,Unit],
    sender : theMDS.Generator[Unit,A,Unit]
  ) 

  class AMQPQueueM[A](
    exchange : String,
    routingKey : String
  ) extends StdMonadicAMQPDispatcher[A]( host, port )
  with ForNotationAdapter[AMQPQueue,A] 
  with BMonad[AMQPQueue]
  with MonadPlus[AMQPQueue]
  with MonadFilter[AMQPQueue] {
    def sender [A] ( 
      exchange : String,
      routingKey : String
    ) : theMDS.Generator[Unit, A, Unit] = {
      theMDS.Generator {
	k : ( Unit => A @suspendable ) => {
	  for( channel <- senderChannel( exchange, routingKey ) ) {
	    spawn {
	      val qname = ( exchange + "_queue" )
		channel.exchangeDeclare( exchange, "direct" )
	      channel.queueDeclare( qname, true, false, false, null );
	      channel.queueBind( qname, exchange, routingKey )
	      
	      val bytes = new ByteArrayOutputStream
	      val store = new ObjectOutputStream(bytes)
	      store.writeObject( k() )
	      store.close
	      channel.basicPublish(
		exchange,
		routingKey,
		null,
		bytes.toByteArray
	      )	    
	    }
	  }
	}
      }
    }
    
    def senderChannel [A] ( 
      exchange : String,
      routingKey : String
    ) : theMDS.Generator[Channel, Unit, Unit] = {
      theMDS.Generator {
	k : ( Channel => Unit @suspendable ) => {
	  val conn = factory.newConnection( Array { new Address(host, port) } )
	  val channel = conn.createChannel()
	  k( channel )
	}
      }
    }
    
    override def unit [S] ( s : S ) : AMQPQueue[S] = {
      val rslt = zero[S]
      spawn {
	reset { for( e <- rslt.sender ) { s } }
      }
      rslt
    }
    override def bind [S,T] (
      amqpS : AMQPQueue[S],
      f : S => AMQPQueue[T]
    ) : AMQPQueue[T] = {
      val acc = zero[T]
      spawn {
	for( s <- amqpS.dispatcher ) {
	  val amqpT = f( s )
	  spawn {
	    for( t <- amqpT.dispatcher ) {
	      for( e <- acc.sender ) { t }
	    }
	  }		
	}
      }
      acc
    }
    override def zero [A] : AMQPQueue[A] = {
      AMQPQueue[A](
	exchange,
	routingKey,
	theMDS.serve[A]( factory, host, port, exchange ),
	sender( exchange, routingKey )
      )
    }
    override def plus [A] (
      amqpA1 : AMQPQueue[A],
      amqpA2 : AMQPQueue[A]
    ) : AMQPQueue[A] = {
      val amqpA12 = zero[A]
      spawn {
	for( a1 <- amqpA1.dispatcher ) {
	  for( e <- amqpA12.sender ) { a1 }
	}
      }
      spawn {
	for( a2 <- amqpA2.dispatcher ) {
	  for( e <- amqpA12.sender ) { a2 }
	}
      }
      amqpA12
    }
    override def mfilter [S] (
      amqpS : AMQPQueue[S],
      pred : S => Boolean
    ) : AMQPQueue[S] = {
      val rslt = zero[S]
      spawn {
	for( s <- amqpS.dispatcher ) {
	  if ( pred( s ) ) {
	    for( e <- rslt.sender ) { s }
	  }
	}
      }
      rslt
    }
  }
}
