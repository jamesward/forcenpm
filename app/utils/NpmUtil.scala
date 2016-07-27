package utils

import java.io.ByteArrayOutputStream
import java.net.URL
import javax.inject.Inject

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveOutputStream}
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WSClient
import resource.ManagedResource

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class NpmUtil @Inject() (implicit ec: ExecutionContext, ws: WSClient) {

  val BASE_URL = "http://registry.npmjs.org"

  // a whole lot of WTF
  private def registryMetadataUrl(packageName: String, maybeVersion: Option[String] = None): String = {
    maybeVersion.fold {
      // when a version is not specified an @ must not be encoded
      val encodedPackageName = packageName.replaceAllLiterally("/", "%2F")
      s"$BASE_URL/$encodedPackageName"
    } { version =>
      // when a version is specified an @ must be encoded
      val encodedPackageName = packageName.replaceAllLiterally("/", "%2F").replaceAllLiterally("@", "%40")
      s"$BASE_URL/$encodedPackageName/$version"
    }
  }

  private def isScoped(maybeScopeAndPackageName: String): Boolean = {
    maybeScopeAndPackageName.contains('/') && maybeScopeAndPackageName.startsWith("@")
  }

  private def registryTgzUrl(maybeScopeAndPackageName: String, version: String): String = {
    if (isScoped(maybeScopeAndPackageName)) {
      val parts = maybeScopeAndPackageName.split('/')
      val scope = parts.head
      val packageName = parts.last
      s"$BASE_URL/$scope/$packageName/-/$packageName-$version.tgz"
    }
    else {
      s"$BASE_URL/$maybeScopeAndPackageName/-/$maybeScopeAndPackageName-$version.tgz"
    }
  }

  def search(keyword: String): Future[JsValue] = {
    ws.url(s"https://ac.cnstrc.com/autocomplete/$keyword?autocomplete_key=CD06z4gVeqSXRiDL2ZNK").get().flatMap { response =>
      response.status match {
        case Status.OK =>
          Future.successful(response.json)
        case _ =>
          Future.failed(new Exception(response.body))
      }
    }
  }

  def versions(name: String): Future[Seq[String]] = {
    ws.url(registryMetadataUrl(name)).get().flatMap { response =>
      response.status match {
        case Status.OK =>
          val versions = (response.json \ "versions").as[Map[String, JsObject]].keys.toIndexedSeq.sorted.reverse
          Future.successful(versions)
        case _ =>
          Future.failed(new Exception(response.body))
      }
    }
  }

  def zip(name: String, version: String): ManagedResource[Array[Byte]] = {
    import resource._

    val url = new URL(registryTgzUrl(name, version))

    for {
      inputStream <- managed(url.openConnection().getInputStream)
      gzipInputStream <- managed(new GzipCompressorInputStream(inputStream))
      tarArchiveInputStream <- managed(new TarArchiveInputStream(gzipInputStream))
      zipByteArrayOutputStream <- managed(new ByteArrayOutputStream())
      zipOutputStream <- managed(new ZipArchiveOutputStream(zipByteArrayOutputStream))
    } yield {
      Stream.continually(tarArchiveInputStream.getNextTarEntry).takeWhile(_ != null).foreach { entry =>
        val fileName = entry.getName.stripPrefix("package/")
        val zipEntry = new ZipArchiveEntry(fileName)
        zipOutputStream.putArchiveEntry(zipEntry)
        IOUtils.copy(tarArchiveInputStream, zipOutputStream)
        zipOutputStream.closeArchiveEntry()
      }

      zipOutputStream.finish()

      zipByteArrayOutputStream.toByteArray
    }
  }

}
