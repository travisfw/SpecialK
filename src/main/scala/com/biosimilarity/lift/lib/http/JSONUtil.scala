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
      
//   def getRspDataBug [T] ( jsonRsp : String, key : String ) : Option[T] = {
//     import scala.collection.JavaConverters._

//     for(	
//       rspData <- toMap( jsonRsp ).get( "data" );
//       v <- rspData.asInstanceOf[java.util.LinkedHashMap[String,Object]].asScala.get( key )
//     ) {
//       v.asInstanceOf[T]
//     }
//   }

  def getRspData [T] ( jsonRsp : String, key : String ) : Option[T] = {
    import scala.collection.JavaConverters._

    toMap( jsonRsp ).get( "data" ) match {
      case Some( rspData ) => {
	rspData.asInstanceOf[java.util.LinkedHashMap[String,Object]].asScala.get( key ) match {
	  case Some( v ) => Some( v.asInstanceOf[T] )
	  case _ => None
	}
      }
      case _ => None
    }    
  }
    
}


