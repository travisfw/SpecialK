// -*- mode: Scala;-*- 
// Filename:    JSONUtil.scala 
// Authors:     lgm                                                    
// Creation:    Tue Dec 13 10:50:19 2011 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.lib.jsonutil

import scala.collection.Map

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
      
  // def getRspDataBug [T] ( jsonRsp : String, key : String ) : Option[T] = {
//     import scala.collection.JavaConverters._

//     val map : scala.collection.Map[String,Object] = toMap( jsonRsp )
//     val oRspData : Option[Object] = map.get( "data" )

//     for(	
//       rspData <- oRspData;
//       oT <- rspData.asInstanceOf[java.util.LinkedHashMap[String,Object]].asScala.get( key )
//     ) {
//       oT.asInstanceOf[T]
//     }
//   }

  def getRspData [T] ( jsonRsp : String, key : String ) : Option[T] = {
    import scala.collection.JavaConverters._

    toMap( jsonRsp ).get( "data" ) match {
      case Some( rspData ) => {
	val rspDataMap =
	  rspData.asInstanceOf[java.util.LinkedHashMap[String,Object]].asScala
	rspDataMap.get( key ) match {
	  case Some( t ) => Some( t.asInstanceOf[T] )
	  case _ => None
	}
      }
      case _ => None
    }    
  }
    
}


