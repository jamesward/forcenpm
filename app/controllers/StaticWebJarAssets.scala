package controllers

import javax.inject.Inject

import controllers.routes.{StaticWebJarAssets => AssetRoutes}
import play.api.Configuration
import play.api.mvc.Controller

class StaticWebJarAssets @Inject() (webJarAssets: WebJarAssets, configuration: Configuration) extends Controller {

  def at(file: String) = Assets.at("/META-INF/resources/webjars", file)
  def atNpm(file: String) = Assets.at("/META-INF/resources/webjars", file)

  lazy val maybeAssetsUrl = configuration.getString("assets.url")

  def getUrl(webJar: String, path: String) = {
    val atUrl = AssetRoutes.at(webJarAssets.fullPath(webJar, path)).url
    maybeAssetsUrl.fold(atUrl)(_ + atUrl)
  }

  def getUrlNpm(webJar: String, path: String) = {
    val atUrl = AssetRoutes.atNpm(webJarAssets.fullPath(webJar, path)).url
    maybeAssetsUrl.fold(atUrl)(_ + atUrl)
  }

  def fullPath(webJar: String, path: String) = {
    webJarAssets.fullPath(webJar, path)
  }

}
