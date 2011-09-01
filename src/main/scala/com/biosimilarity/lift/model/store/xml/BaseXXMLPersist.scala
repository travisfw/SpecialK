// -*- mode: Scala;-*- 
// Filename:    BaseXXMLPersist.scala 
// Authors:     lgm                                                    
// Creation:    Thu Mar 24 10:45:35 2011 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.model.store.xml

import com.biosimilarity.lift.model.ApplicationDefaults
import com.biosimilarity.lift.model.store.CnxnCtxtLabel
import com.biosimilarity.lift.lib._

import org.basex.api.xmldb.BXCollection
import org.basex.BaseXClient
import org.basex.server.ClientSession
import org.basex.core.BaseXException
import org.basex.core.Context
import org.basex.core.cmd.{List => BXListx, _}

import org.xmldb.api.DatabaseManager
import org.xmldb.api.base.Database
import scala.xml._

import javax.xml.transform.OutputKeys
import java.util.UUID
import java.io.File

trait BaseXXMLStore extends BaseXPersist
{
  self: //Reporting
    //with
          ConfigurationTrampoline  =>

  def driverClass : Class[_] = {
    Class.forName( driver )
  }

  def database : Database = {
    val db = driverClass.newInstance().asInstanceOf[Database]
    DatabaseManager.registerDatabase( db )
    if ( createDB ) {
      db.setProperty( "create-database", createDB.toString )
    }
    db
  }
}

trait BaseXCnxnStorage[Namespace, Var, Tag]
  extends BaseXPersist
  with XMLIfy[Namespace, Var]
{
  self: BaseXXMLStore
    //with Reporting
    with ConfigurationTrampoline
    with UUIDOps =>

  override def tmpDirStr: String =
  {
    throw new Exception("don't use the filebased api")
  }

  def store(xmlCollStr: String)(
    cnxn: CnxnCtxtLabel[Namespace, Var, String]
    ): Unit =
  {
    val (k, v) = keyValue(cnxn)
    insertUpdate(xmlCollStr, k.toString, v.toString)
  }

  def keyValue(
    cnxn: CnxnCtxtLabel[Namespace, Var, String]
    ): (Node, Node) =
  {
    val cxml = xmlIfier.asXML(cnxn)
    cxml.child.toList match {
      case k :: v :: Nil => {
        (k, v)
      }
      case _ => {
        throw new Exception("malformed record: " + cxml)
      }
    }
  }

  def delete(xmlCollStr: String, path: CnxnCtxtLabel[Namespace, Var, String]
    ): Unit =
  {
    val key = xmlIfier.asXML(path)
    delete(xmlCollStr, key.toString)
  }

//  def update(xmlCollStr: String)(
//    cnxn: CnxnCtxtLabel[Namespace, Var, String]
//    ): Unit =
//  {
//     xmlIfier.asXML(cnxn).toString
//     val (k, v) = keyValue(cnxn)
//     val node = xmlIfier.asXML(cnxn)
//     update(xmlCollStr, k.toString, v.toString)
//  }
}
