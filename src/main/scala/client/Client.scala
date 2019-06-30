package client

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.{Cookie, HttpCookiePair}
import akka.http.scaladsl.model.{HttpMethod, HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import com.typesafe.sslconfig.akka.AkkaSSLConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class Client(
              implicit actorSystem: ActorSystem,
              materializer: ActorMaterializer,
            ) {

  private val innerClient: HttpExt = Http()
  this.innerClient.setDefaultClientHttpsContext(
    this.innerClient.createClientHttpsContext(
      AkkaSSLConfig().mapSettings(
        settings => settings
          .withLoose(
            settings
              .loose
              .withAllowWeakProtocols(true)
              .withAllowWeakCiphers(true)
              .withAllowUnsafeRenegotiation(Some(true))
              .withAcceptAnyCertificate(true)
            // .withDisableHostnameVerification(true)
            // .withDisableSNI(true)
          )
      )
    )
  )

  def getHTML(url: String, method: HttpMethod = HttpMethods.GET)(implicit cookies: List[HttpCookiePair] = List.empty): Future[Element] = {
    val response = GET(url, method)
    response.map(Jsoup.parseBodyFragment(_).body())
  }

  def getJSON(url: String)(implicit cookies: List[HttpCookiePair] = List.empty): Future[JsValue] = {
    val response = GET(url)
    response.map(_.parseJson)
  }

  private def GET(url: String, method: HttpMethod = HttpMethods.GET)(implicit cookies: List[HttpCookiePair] = List.empty): Future[String] = {
    val headers = if (cookies.isEmpty) List.empty else List(Cookie(cookies))

    val request = HttpRequest(
      method = method,
      uri = url,
      headers = headers
    )
    println(request.cookies)

    val response = this.innerClient.singleRequest(request = request)
    response.flatMap(r => Unmarshal(r).to[String])
  }
}
