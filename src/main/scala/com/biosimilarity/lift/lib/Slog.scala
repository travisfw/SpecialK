// -*- mode: Scala;-*- 
// Filename:    Slog.scala 
// Authors:     lgm                                                    
// Creation:    Wed Sep  8 11:17:09 2010 
// Copyright:   Not supplied 
// Description: 
// ------------------------------------------------------------------------

package com.biosimilarity.lift.lib

import net.lag.configgy._
import net.lag.logging.{Logger => ConfiggyLogger, _ }

import org.apache.log4j.{PropertyConfigurator, Level, Logger}

import scala.xml._
import java.util.UUID

trait WireTap {
  def tap [A] ( fact : A ) : Unit
}


object JournalIDVender extends UUIDOps

trait Verbosity {
    def id : UUID
  }
class Luddite(
  override val id : UUID
) extends Verbosity
class Blogger(
  override val id : UUID
) extends Luddite( id )
class Twitterer(
  override val id : UUID
) extends Blogger( id )

object Twitterer {
  def apply(
    id : UUID
  ) : Twitterer = new Twitterer( id )
  def unapply( t : Twitterer ) : Option[(UUID)] =
    Some( (t.id) )
}

object Blogger {
  def apply(
    id : UUID
  ) : Blogger = new Blogger( id )
  def unapply( t : Blogger ) : Option[(UUID)] =
    Some( (t.id) )
}

object Luddite {
  def apply(
    id : UUID
  ) : Luddite = new Luddite( id )
  def unapply( t : Luddite ) : Option[(UUID)] =
    Some( (t.id) )
}

case object TheTwitterer
extends Twitterer(
  JournalIDVender.getUUID
)
case object TheBlogger
extends Blogger(
  JournalIDVender.getUUID
)
case object TheLuddite
extends Luddite(
  JournalIDVender.getUUID
)
  

// This design is now officially baroque-en! Fix it into simplicity, please!
trait Journalist {

  object journalIDVender extends UUIDOps

  lazy val _notebook = new StringBuffer
  def notebook : StringBuffer = _notebook
  
  def displayFn[A] : ( Verbosity, A ) => Unit = {
    ( v : Verbosity, a : A ) => println( a )
  }

  def rememberFn[A] : ( Verbosity, A ) => Unit = {
    ( v : Verbosity, x : A ) => {
      notebook.append( x )
    }
  }

  def storeFn[A] : ( Verbosity, A ) => Unit = {
    ( v : Verbosity, x : A ) => {
      //throw new Exception( "log store not defined" )
    }
  }

  val reportage = report( TheTwitterer ) _

  case class TaggedFact[A]( verb : Verbosity, fact : A )

  def markUp[A]( verb : Verbosity)( fact : A ) =
    TaggedFact( verb, fact )

  def asTweet[A]( fact : A ) = markUp[A]( TheTwitterer )( fact )
  def asBlog[A]( fact : A ) = markUp[A]( TheBlogger )( fact )
  def asRecord[A]( fact : A ) = markUp[A]( TheLuddite )( fact )

  def tweet[A]( fact : A ) =
    report( Twitterer( journalIDVender.getUUID ) )( asTweet( fact ) )
  def blog[A]( fact : A ) =
    report( Blogger( journalIDVender.getUUID ) )( asTweet( fact ) )
  def record[A]( fact : A ) =
    report( Luddite( journalIDVender.getUUID ) )( asTweet( fact ) )

  implicit def exceptionToTraceStr( e : Exception ) : String = {
    val sw = new java.io.StringWriter()
    e.printStackTrace(
      new java.io.PrintWriter(
	sw,
	true
      )
    )
    sw.toString
  }

  def tweetTrace( e : Exception ) = {    
    report( Twitterer( journalIDVender.getUUID ) )(
      asTweet( exceptionToTraceStr( e ) ) 
    )
  }
  def blogTrace( e : Exception ) = {
    report( Blogger( journalIDVender.getUUID ) )(
      asTweet( exceptionToTraceStr( e ) )
    )
  }
  def recordTrace( e : Exception ) = {
    report( Luddite( journalIDVender.getUUID ) )(
      asTweet( exceptionToTraceStr( e ) )
    )
  }

  def tagIt [A]( verb : Verbosity, bite : A ) : Elem = {
    verb match {
      case Twitterer( _ ) => <tweet>{bite}</tweet>
      case Blogger( _ ) => <blog>{bite}</blog>
      case Luddite( _ ) => <record>{bite}</record>
    }
  }

  def report [A] ( verb : Verbosity )( fact : A ) : Unit = {
    fact match {
      case TaggedFact( vrb, bite ) => {
	val rpt = tagIt( vrb, bite )
	if ( verb.getClass.isInstance( vrb ) ) {
	  verb match {
	    case Twitterer( _ ) => {	      
	      displayFn( vrb, rpt )
	      rememberFn( vrb, rpt )
	      storeFn( vrb, rpt )
	    }
	    case Blogger( _ ) => {
	      rememberFn( vrb, rpt )
	      storeFn( vrb, rpt )
	    }
	    case Luddite( _ ) => {
	      storeFn( vrb, rpt )
	    }
	  }
	}
      }
      case _ => {
	val rpt = tagIt( verb, fact )
	verb match {
	  case Twitterer( _ ) => {	    
	    displayFn( verb, rpt )
	    rememberFn( verb, rpt )
	    storeFn( verb, rpt )
	  }
	  case Blogger( _ ) => {
	    rememberFn( verb, rpt )
	    storeFn( verb, rpt )
	  }
	  case Luddite( _ ) => {
	    storeFn( verb, rpt )
	  }
	}
      }
    }    
  }
  
  def report [A] ( fact : A ) : Unit = {
    report( TheTwitterer )( fact )
  }
}

trait ConfiggyReporting {
  self : Journalist =>
  
  def config : Config
  def logger : ConfiggyLogger

  def wrap [A] ( fact : A ) : Elem = {
     <report>{fact}</report>
  }

  def logFatal [A] ( fact : A ) : Unit = {
    logger.ifFatal(wrap(fact) toString)
  }

  def logCritical [A] ( fact : A ) : Unit = {
    logger.ifCritical(wrap(fact) toString)
  }

  def logError [A] ( fact : A ) : Unit = {
    logger.ifError(wrap(fact) toString)
  }
  
  def logWarning [A] ( fact : A ) : Unit = {
    logger.ifWarning(wrap(fact) toString) 
  }
  
  def logInfo [A] ( fact : A ) : Unit = {
    logger.ifInfo(wrap(fact) toString) 
  }
  
  def logDebug [A] ( fact : A ) : Unit = {
    logger.ifDebug(wrap(fact) toString) 
  }

  def logTrace [A] ( fact : A ) : Unit = {
    logger.ifTrace(wrap(fact) toString)
  }
}

trait ConfiggyJournal {
  self : Journalist with ConfiggyReporting =>
    Configgy.configure("log.conf")

  override lazy val config = Configgy.config

  override lazy val logger = ConfiggyLogger.get  

  override def storeFn[A] : ( Verbosity, A ) => Unit = {
    ( v : Verbosity, a : A ) => {
      v match {
	case Twitterer( _ ) => {
	  logger.ifInfo( tagIt( v, a ).toString ) 
	}
	case Blogger( _ ) => {
	  logger.ifTrace( tagIt( v, a ).toString ) 
	}
	case Luddite( _ ) => {
	  logger.ifDebug( tagIt( v, a ).toString ) 
	}
      }
    }
  }
}

object ConfiguredJournalDefaults {
  val loggingLevel = "Tweet"
}

trait ConfiguredJournal {
  self : Journalist
       with ConfiggyReporting
	with ConfigurationTrampoline =>    

  override lazy val config = Configgy.config

  override lazy val logger = ConfiggyLogger.get  

  override def configurationDefaults : ConfigurationDefaults = {
    ConfiguredJournalDefaults.asInstanceOf[ConfigurationDefaults]
  }

  def loggingLevel        : String =
    configurationFromFile.get( "loggingLevel" ).getOrElse( bail() )

  var _loggingLevel : Option[Verbosity] = None
  def setLoggingLevel( verb : Verbosity ) : Unit = {
    _loggingLevel = Some( verb )
  }
  def getLoggingLevel : Verbosity = {
    _loggingLevel match {
      case Some( verb ) => verb
      case None => {
	loggingLevel match {
	  case "Tweet" => {
	    val ll = Twitterer( journalIDVender.getUUID )
	    _loggingLevel = Some( ll )
	    ll
	  }
	  case "Blog" => {
	    val ll = Blogger( journalIDVender.getUUID )
	    _loggingLevel = Some( ll )
	    ll
	  }
	  case "Record" => {
	    val ll = Luddite( journalIDVender.getUUID )
	    _loggingLevel = Some( ll )
	    ll
	  }
	}
      }
    }
  }

  override def tweet[A]( fact : A ) =
    report( getLoggingLevel )( asTweet( fact ) )
  override def blog[A]( fact : A ) =
    report( getLoggingLevel )( asTweet( fact ) )
  override def record[A]( fact : A ) =
    report( getLoggingLevel )( asTweet( fact ) )

  override def storeFn[A] : ( Verbosity, A ) => Unit = {
    ( v : Verbosity, a : A ) => {
      v match {
	case Twitterer( _ ) => {
	  logger.ifInfo( tagIt( v, a ).toString ) 
	}
	case Blogger( _ ) => {
	  logger.ifTrace( tagIt( v, a ).toString ) 
	}
	case Luddite( _ ) => {
	  logger.ifDebug( tagIt( v, a ).toString ) 
	}
      }
    }
  }
}

abstract class Reporter( override val notebook : StringBuffer )
	 extends Journalist

class ConfiggyReporter(
  override val notebook : StringBuffer
) extends Reporter( notebook )
  with Journalist
  with ConfiggyReporting	 
  with ConfiggyJournal {
}

object Severity extends Enumeration()
{
  type Severity = Value
  val Fatal, Error, Warning, Info, Debug, Trace = Value
}

//call monitoring?
trait Reporting
{
  def SeverityFromOption(level: Option[ String ]): Severity.Value =
  {
    level match {
      case Some(x) => {
        SeverityFromString(x)
      }
      case None => {
        Severity.Debug
      }
    }
  }

  def SeverityFromString(level: String): Severity.Value =
  {
    level.toLowerCase() match {
      case "fatal" => {
        Severity.Fatal
      }
      case "error" => {
        Severity.Error
      }
      case "warning" => {
        Severity.Warning
      }
      case "info" => {
        Severity.Info
      }
      case "debug" => {
        Severity.Debug
      }
      case "trace" => {
        Severity.Trace
      }
      case _ => {
        Severity.Debug
      }
    }
  }

  def prettyPrint(value: String): String =
  {
    value.replace("{", "")
      .replace("}", "")
      .replace("&amp;", "")
      .replace("vamp;", "")
      .replace("amp;", "")
      .replace("&quot;", "")
      .replace("vquot;", "")
      .replace("quot;", "")
      .replace("_-", "")
      .replace(":", "")
      .replace("@class", "")
      .replace("com.biosimilarity.lift.model.store.", "")
      .replace("com.protegra.agentservices.store.", "")
      .replace("MonadicTermTypes", "")
      .replace("AgentTS$TheMTT$", "")
      .replace("Groundvstring,$", "")
      .replace("Groundstring,$", "")
      .replace(",outer", "")
      .replace("&lt;", "<")
      .replace("&gt;", ">")
      .toString
  }

  Configgy.configure("log.conf")
  PropertyConfigurator.configure("log.properties")

  lazy val config = Configgy.config
  var tweetLevel = SeverityFromOption(config.getString("tweetLevel"))
  var blogLevel = SeverityFromOption(config.getString("blogLevel"))

  lazy val logger = Logger.getLogger(this.getClass.getName)

  def header(level: Severity.Value): String =
  {
    "=" + level.toString.toUpperCase + " REPORT==== Thread " + Thread.currentThread.getName + " ==="
  }

  def wrap[ A ](fact: A): String =
  {
    //keep it readable on println but still send it all to the log
    val value = prettyPrint(fact.toString)
    val max = if ( value.length < 512 ) value.length else 512
    value.substring(0, max)
  }

  def enabled(reportLevel: Severity.Value, configLevel: Severity.Value): Boolean =
  {
    //use id to compare ints in order of declaration
    reportLevel.id <= configLevel.id
  }

  def report[ A ](fact: A): Unit =
  {
    report(fact, Severity.Debug)
  }

  def report[ A ](fact: A, level: Severity.Value) =
  {
    tweet(fact, level)
    blog(fact, level)
  }

  def tweet[ A ](fact: A): Unit =
  {
    tweet(fact, Severity.Debug)
  }

  def tweet[ A ](fact: A, level: Severity.Value) =
  {
    if ( enabled(level, tweetLevel) ) {trace(fact, level)}
  }

  private def trace[ A ](fact: A, level: Severity.Value) =
  {
    //todo: worth adding severity to output <report> tag?
    level match {
      case _ => {
        println(header(level) + "\n" + wrap(fact).toString() + "\n")
      }
    }
  }

  def blog[ A ](fact: A): Unit =
  {
    blog(fact, Severity.Debug)
  }

  def blog[ A ](fact: A, level: Severity.Value) =
  {
    if ( enabled(level, blogLevel) ) {log(fact, level)}
  }

  private def log[ A ](fact: A, level: Severity.Value) =
  {
    level match {
      case Severity.Fatal => {
        logger.log(Level.FATAL, fact toString)
      }
      case Severity.Error => {
        logger.log(Level.ERROR, fact toString)
      }
      case Severity.Warning => {
        logger.log(Level.WARN, fact toString)
      }
      case Severity.Info => {
        logger.log(Level.INFO, fact toString)
      }
      case Severity.Debug => {
        logger.log(Level.DEBUG, fact toString)
      }
      case Severity.Trace => {
        logger.log(Level.TRACE, fact toString)
      }
      case _ => {
        logger.log(Level.DEBUG, fact toString)
      }
    }
  }

  //  implicit def exceptionToTraceStr( e : Exception ) : String = {
  //    val sw = new java.io.StringWriter()
  //    e.printStackTrace(
  //      new java.io.PrintWriter(
  //	sw,
  //	true
  //      )
  //    )
  //    sw.toString
  //  }

}
