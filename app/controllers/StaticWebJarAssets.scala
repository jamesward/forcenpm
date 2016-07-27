package controllers

import javax.inject.Inject

import play.api.Configuration
import play.api.mvc.Controller
import org.webjars.RequireJS

class StaticWebJarAssets @Inject() (webJarAssets: WebJarAssets, configuration: Configuration) extends Controller {

  import java.util

  import com.fasterxml.jackson.databind.node.ObjectNode

  // prepends a url if the assets.url config is set
  def url(file: String): String = {
    val baseUrl = routes.WebJarAssets.at(file).url
    configuration.getString("assets.url").fold(baseUrl)(_ + baseUrl)
  }

  def urlFullPath(webJar: String, path: String): String = {
    url(webJarAssets.fullPath(webJar, path))
  }

  def requireJsConfig: util.Collection[ObjectNode] = {
    configuration.getString("assets.url").fold {
      RequireJS.getSetupJson(url(""))
    } { assetsUrl =>
      RequireJS.getSetupJson(url(""), routes.WebJarAssets.at("").url)
    }.values()
  }

}
