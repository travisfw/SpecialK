package com.biosimilarity.lift.lib.extensions
import com.biosimilarity.lift.lib.moniker._

import java.net.URI

object URIExtensions
{
  implicit def uriExt(source: URI) = new URIExt(source)

  class URIExt( source: URI ) {
    lazy val muri : MURI = MURI( source )
    def asMoniker() = muri
    def withPort(port: Int) : URI = {
      new URI(
        source.getScheme(),
        source.getUserInfo(),
        source.getHost(),
        port,
        source.getPath(),
        source.getQuery(),
        source.getFragment()
      )
    }

    def withPath(path: String) : URI = {
      new URI(
        source.getScheme(),
        source.getUserInfo(),
        source.getHost(),
        source.getPort(),
        path,
        source.getQuery(),
        source.getFragment()
      )
    }
  }
}
