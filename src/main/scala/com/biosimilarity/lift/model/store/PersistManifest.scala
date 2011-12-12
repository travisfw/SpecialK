package com.biosimilarity.lift.model.store

//one implementation
//Pattern = mTT.GetRequest
//Resource = mTT.Resource
//T = Elem

trait PersistManifest[Pattern, Resource, T, Namespace,Var,Tag,Value]
{
    //def db : Database

    def storeUnitStr[Src,Label,Trgt]( cnxn : Cnxn[Src,Label,Trgt] ) : String
    def storeUnitStr : String

    //deprecated
//    def toFile( ptn : Pattern ) : Option[File]

    def query( ptn : Pattern ) : Option[String]
    def query( xmlCollStr : String, ptn : Pattern ) : Option[String]

    def labelToNS : Option[String => Namespace]
    def textToVar : Option[String => Var]
    def textToTag : Option[String => Tag]        

    def kvNameSpace : Namespace

    def asStoreKey(
      key : Pattern
    ) : CnxnCtxtLabel[Namespace,Var,String] with Factual
  
    def asStoreValue(
      rsrc : Resource
    ) : CnxnCtxtLeaf[Namespace,Var,String] with Factual
    
    def asStoreRecord(
      key : Pattern,
      value : Resource
    ) : CnxnCtxtLabel[Namespace,Var,String] with Factual

    def asCacheValue(
      ccl : CnxnCtxtLabel[Namespace,Var,String]
    ) : Value    

    def asCacheValue(
      ltns : String => Namespace,
      ttv : String => Var,
      value : T
    ) : Option[Value]

    def asValue(
     value: T
    ): Option[ Value ]

    def recordDeletionQueryTemplate : String
}
