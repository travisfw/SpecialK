package com.biosimilarity.lift.model.store
import xml._
import com.biosimilarity.lift.lib._

import scala.xml._

//one implementation
//Pattern = Pattern
//Resource = Resource
//T = Elem

//refactor to not require UUIDOps? & Blobify? not really needed for CnxnXQuery
//CnxnXQuery introduces a lot of self type additions

trait XMLPersistManifest[Pattern, Resource, T <: Node, Namespace,Var,Tag,Value]
  extends PersistManifest[Pattern, Resource, T, Namespace,Var,Tag,Value]
  with MonadicGenerators
{
  self : CnxnXQuery[Namespace,Var,Tag]
    with CnxnXML[Namespace,Var,Tag]
    with Blobify
    with CnxnCtxtInjector[Namespace,Var,Tag]
    with XMLIfy[Namespace,Var]
    with UUIDOps =>

  def asCCL(
    gReq : Pattern
  ) : CnxnCtxtLabel[Namespace,Var,Tag] with Factual = {
    gReq.asInstanceOf[CnxnCtxtLabel[Namespace,Var,Tag] with Factual]
  }

   def storeUnitStr[Src,Label,Trgt]( cnxn : Cnxn[Src,Label,Trgt] ) : String
   def storeUnitStr : String

   def query(
      ptn : Pattern
    ) : Option[String] = {
      for( ttv <- textToVar )
	yield {
	  xqQuery(
	    new CnxnCtxtBranch[Namespace,Var,Tag](
	      kvNameSpace,
	      List(
		asCCL( ptn ),
		new CnxnCtxtLeaf[Namespace,Var,Tag](
		  Right(
		    ttv( "VisForValueVariableUniqueness" )
		  )
		)
	      )
	    )
	  )
	}
    }

    def query(
      xmlCollStr : String,
      ptn : Pattern
    ) : Option[String] = {
      for( ttv <- textToVar )
	yield {
	  val ccb =
	    new CnxnCtxtBranch[Namespace,Var,Tag](
	      kvNameSpace,
	      List(
		asCCL( ptn ),
		new CnxnCtxtLeaf[Namespace,Var,Tag](
		  Right(
		    ttv( "VisForValueVariableUniqueness" )
		  )
		)
	      )
	    )
	  xqQuery(
	    ccb,
	    XQCC(
	      Some( ccb ),
	      ccb,
	      "collection( '%COLLNAME%' )/".replace(
		"%COLLNAME%",
		xmlCollStr
	      ),
	      Some( nextXQV ),
	      None, None,
	      DeBruijnIndex( 0, 0 )
	    )
	  )
	}
    }

    def asStoreKey(
      key : Pattern
    ) : CnxnCtxtLabel[Namespace,Var,String] with Factual = {
      key match {
	case CnxnCtxtLeaf( Left( t ) ) =>
	  new CnxnCtxtLeaf[Namespace,Var,String](
	    Left( t + "" )
	  )
	case CnxnCtxtLeaf( Right( v: Var ) ) =>
	  new CnxnCtxtLeaf[Namespace,Var,String](
	    Right( v )
	  )
	case CnxnCtxtBranch( ns: Namespace, facts: List[Pattern] ) =>
	  new CnxnCtxtBranch[Namespace,Var,String](
	    ns,
            facts.map( asStoreKey )
	  )
      }
    }

    def asStoreRecord(
      key : Pattern,
      value : Resource
    ) : CnxnCtxtLabel[Namespace,Var,String] with Factual = {
      new CnxnCtxtBranch[Namespace,Var,String](
	kvNameSpace,
	List( asStoreKey( key ), asStoreValue( value ) )
      )
    }

    def asCacheValue(
      ccl : CnxnCtxtLabel[Namespace,Var,String]
    ) : Value

    def asCacheValue(
      ltns : String => Namespace,
      ttv : String => Var,
      value : T
    ) : Option[Value] = {
      val ttt = ( x : String ) => x
      xmlIfier.fromXML( ltns, ttv, ttt )( value ) match {
	case Some( CnxnCtxtBranch( ns, k :: v :: Nil ) ) => {
	  val value : Value =
	    asCacheValue(
	      v.asInstanceOf[CnxnCtxtLabel[Namespace,Var,String]]
	    )
	  if ( kvNameSpace.equals( ns ) ) {
	    Some( value )
	  }
	  else {
	    None
	  }
	}
	case v@_ => {
	  None
	}
      }
    }

  def asValue(
     value: T
     ): Option[ Value ] =
   {
     for (
       ltns <- labelToNS;
       ttv <- textToVar;
       //BUGBUG: is the ttt needed
       ttt <- textToTag;
       vCCL <- asCacheValue(ltns, ttv, value)
     ) yield {
        vCCL
     }
   }

//    override def toFile(
//      ptn : Pattern
//    ) : Option[File] = {
//      // TBD
//      None
//    }

    def recordDeletionQueryTemplate : String = {
      "delete node let $key := %RecordKeyConstraints% for $rcrd in collection( '%COLLNAME%' )//record where deep-equal($rcrd/*[1], $key) return $rcrd"
    }
}
