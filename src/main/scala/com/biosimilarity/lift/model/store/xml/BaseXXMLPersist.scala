// -*- mode: Scala;-*- 
// Filename:    BaseXXMLPersist.scala 
// Authors:     lgm                                                    
// Creation:    Thu Mar 24 10:45:35 2011 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.model.store.xml

import com.biosimilarity.lift.model.ApplicationDefaults
import com.biosimilarity.lift.model.store.CnxnLabel
import com.biosimilarity.lift.model.store.OntologicalStatus
import com.biosimilarity.lift.model.store.Factual
import com.biosimilarity.lift.model.store.Hypothetical
import com.biosimilarity.lift.model.store.Theoretical
import com.biosimilarity.lift.model.store.CnxnLeaf
import com.biosimilarity.lift.model.store.CCnxnLeaf
import com.biosimilarity.lift.model.store.AbstractCnxnBranch
import com.biosimilarity.lift.model.store.CnxnBranch
import com.biosimilarity.lift.model.store.CCnxnBranch
import com.biosimilarity.lift.model.store.CnxnCtxtLabel
import com.biosimilarity.lift.model.store.CnxnCtxtLeaf
import com.biosimilarity.lift.model.store.CCnxnCtxtLeaf
import com.biosimilarity.lift.model.store.AbstractCnxnCtxtBranch
import com.biosimilarity.lift.model.store.CnxnCtxtBranch
import com.biosimilarity.lift.model.store.CCnxnCtxtBranch
import com.biosimilarity.lift.model.store.CnxnCtxtInjector
import com.biosimilarity.lift.model.store.Cnxn
import com.biosimilarity.lift.model.store.CCnxn
import com.biosimilarity.lift.model.store.CnxnXML
import com.biosimilarity.lift.model.store.Blobify
import com.biosimilarity.lift.model.store.CnxnXQuery
import com.biosimilarity.lift.lib._

import org.basex.api.xmldb.BXCollection
import org.basex.BaseXClient
import org.basex.server.ClientSession
import org.basex.core.BaseXException
import org.basex.core.Context
import org.basex.core.cmd.{ List => BXListx ,_ }
import org.basex.data.Result
import org.basex.data.XMLSerializer
import org.basex.query.QueryException
import org.basex.query.QueryProcessor
import org.basex.query.item.Item
import org.basex.query.iter.Iter

import org.xmldb.api.base._
import org.xmldb.api.modules._
import org.xmldb.api._

import scala.xml._

import javax.xml.transform.OutputKeys
import java.util.UUID
import java.io.File

trait BaseXXMLStore extends BaseXPersist {
  self : /* Journalist
	 with ConfiggyReporting
	 with ConfiguredJournal */
         ConfigurationTrampoline
	 /* with UUIDOps */ =>

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

trait BaseXCnxnStorage[Namespace,Var,Tag]
    extends BaseXPersist
    with XMLIfy[Namespace,Var] {
  self : BaseXXMLStore
        /* with Journalist
	with ConfiggyReporting
	with ConfiguredJournal */
        with ConfigurationTrampoline
	with UUIDOps =>
    
  override def tmpDirStr : String = {
      throw new Exception( "don't use the filebased api" )
    }      
  
  def store(xmlCollStr: String)(
    cnxn: CnxnCtxtLabel[Namespace, Var, String]
    ): Unit =
  {
    val (k, v) = keyValue(cnxn)
    insertUpdate(xmlCollStr, k.toString, v.toString)
  }  

  def keyValue(
    cnxn : CnxnCtxtLabel[Namespace,Var,String]
  ) : ( Node, Node ) = {
    val cxml = xmlIfier.asXML( cnxn )
    cxml.child.toList match {
      case k :: v :: Nil => {
	( k, v )
      }
      case _ => {
	throw new Exception( "malformed record: " + cxml )
      }
    }
  }

 def delete(xmlCollStr: String, path: CnxnCtxtLabel[Namespace, Var, String]
    ): Unit =
  {
    val key = xmlIfier.asXML(path)
    delete(xmlCollStr, key.toString)
  }
}

package usage {

class BaseXRetrieveExample
extends Journalist
with ConfiggyReporting
with ConfiggyJournal
with UUIDOps {
  def get(
    xmlColl : String,
    xmlRsrc : String,
    xmlPath : String
  ) : Unit = {
    val context : Context = new Context();
 
    reportage( "=== QueryCollection ===" )
 
    // ------------------------------------------------------------------------
    // Create a collection from all XML documents in the 'etc' directory
    reportage( "\n* Create a collection." )
 
    new CreateDB( xmlColl, xmlPath ).execute( context )
 
    // ------------------------------------------------------------------------
    // List all documents in the database
    reportage( "\n* List all documents in the database:" )
 
    // The XQuery base-uri() function returns a file path
    reportage(
      new XQuery(
        "for $doc in collection('" + xmlColl + "')" +
        "return <doc path='{ base-uri($doc) }'/>"
      ).execute( context )
    )
 
    // ------------------------------------------------------------------------
    // Evaluate a query on a single document
    reportage( "\n* Evaluate a query on a single document:" )
 
    // If the name of the database is omitted in the collection() function,
    // the currently opened database will be referenced
    reportage(
      new XQuery(
        "for $doc in collection()" +
        "let $file-path := base-uri($doc)" +
        "where ends-with($file-path, '" + xmlRsrc + "')" +
        "return concat($file-path, ' has ', count($doc//*), ' elements')"
      ).execute( context )
    )
 
    // ------------------------------------------------------------------------
    // Drop the database
    reportage( "\n* Drop the database." )
 
    new DropDB( xmlColl ).execute( context )
 
    // ------------------------------------------------------------------------
    // Close the database context
    context.close()
  }
}

}
