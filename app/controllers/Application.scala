package controllers

import javax.inject.Inject

import controllers.Application._
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.mvc._
import play.api.{Environment, Mode}
import utils.{AuthInfo, ForceUtil, NpmUtil, RequestError}

import scala.concurrent.{ExecutionContext, Future}


class Application @Inject() (force: ForceUtil, npm: NpmUtil, environment: Environment) (implicit staticWebJarAssets: StaticWebJarAssets, ec: ExecutionContext) extends Controller {

  val X_ID_URL = "X-ID-URL"
  val X_ACCESS_TOKEN = "X-ACCESS-TOKEN"
  val X_REFRESH_TOKEN = "X-REFRESH-TOKEN"
  val X_INSTANCE_URL = "X-INSTANCE-URL"

  private def errorsToJson(errors: Seq[(JsPath, Seq[ValidationError])]): JsObject = {
    Json.obj("errors" -> errors.toString())
  }

  class RequestWithAuthInfo[A](val authInfo: AuthInfo, request: Request[A]) extends WrappedRequest[A](request)

  object AuthInfoAction extends ActionBuilder[RequestWithAuthInfo] with ActionRefiner[Request, RequestWithAuthInfo] {

    override def refine[A](request: Request[A]): Future[Either[Result, RequestWithAuthInfo[A]]] = {
      val maybeAuthInfo = for {
        idUrl <- request.state(X_ID_URL)
        accessToken <- request.state(X_ACCESS_TOKEN)
        refreshToken <- request.state(X_REFRESH_TOKEN)
        instanceUrl <- request.state(X_INSTANCE_URL)
      } yield AuthInfo(idUrl, accessToken, refreshToken, instanceUrl)

      val validAuthInfoFuture = maybeAuthInfo.fold(Future.failed[AuthInfo](new Exception("No Auth Info"))) { authInfo =>
        force.userInfo(authInfo).map(_ => authInfo)
      } recoverWith {
        case e: RequestError if e.message == "Bad_OAuth_Token" =>
          // try to refresh the token
          Future.failed(e)
      }

      validAuthInfoFuture.map { authInfo =>
        Right(new RequestWithAuthInfo(authInfo, request))
      } recover {
        case e: Exception =>
          Left {
            render {
              case Accepts.Html() => Redirect(routes.Application.login())
              case Accepts.Json() => Unauthorized(Json.obj("error" -> s"The auth info was not set"))
            } (request)
          }
      }
    }
  }

  def index() = AuthInfoAction { request =>
    Ok(views.html.index(request.authInfo, environment.mode))
  }

  def login() = Action {
    Ok(views.html.login())
  }

  def forceUserInfo() = AuthInfoAction.async { request =>
    val userOrgInfoFuture = for {
      userInfo <- force.userInfo(request.authInfo)
      orgId = (userInfo \ "organization_id").as[String]
      orgInfo <- force.orgInfo(request.authInfo, orgId)
    } yield {
      userInfo.as[JsObject] ++ orgInfo.as[JsObject]
    }

    userOrgInfoFuture.map { json =>
      Ok(json)
    }
  }

  def forceNpmList() = AuthInfoAction.async { request =>
    force.resourcesWithPrefix(request.authInfo, "npm_").map { json =>
      Ok(json)
    }
  }

  def forceNpmCreate(name: String, version: String) = AuthInfoAction.async { request =>
    npm.zip(name, version).acquireAndGet { bytes =>
      force.createResource(request.authInfo, name, version, bytes).map { json =>
        Created(json)
      } recover {
        case e: Exception => InternalServerError(e.getMessage)
      }
    }
  }

  def npmPackageSearch(keyword: String) = Action.async {
    npm.search(keyword).map(Ok(_)).recover {
      case e: Exception => InternalServerError(e.getMessage)
    }
  }

  def npmPackageVersions(name: String) = Action.async {
    npm.versions(name).map { versions =>
      Ok(Json.toJson(versions))
    } recover {
      case e: Exception =>
        InternalServerError
    }
  }

  def npmPackageFiles(name: String, version: String) = Action {
    npm.files(name, version).acquireAndGet { files =>
      Ok(Json.toJson(files))
    }
  }

  def oauthLoginProd() = Action { implicit request =>
    Redirect(force.loginUrl(force.ENV_PROD)).flashing(force.SALESFORCE_ENV -> force.ENV_PROD)
  }

  def oauthLoginSandbox() = Action { implicit request =>
    Redirect(force.loginUrl(force.ENV_SANDBOX)).flashing(force.SALESFORCE_ENV -> force.ENV_SANDBOX)
  }

  def oauthCallback(code: String) = Action.async { implicit request =>

    val env = request.flash.get(force.SALESFORCE_ENV).getOrElse(force.ENV_PROD)

    val loginFuture = force.login(code, env)

    loginFuture.map { authInfo =>

      val state = Map(
        X_ID_URL -> authInfo.idUrl,
        X_ACCESS_TOKEN -> authInfo.accessToken,
        X_REFRESH_TOKEN -> authInfo.refreshToken,
        X_INSTANCE_URL -> authInfo.instanceUrl
      ).toSeq

      val redirect = Redirect(routes.Application.index())

      environment.mode match {
        case Mode.Dev => redirect.withSession(state:_*)
        case _ => redirect.flashing(state:_*)
      }
    } recover { case e: Error =>
      Redirect(routes.Application.login())
    }
  }

}

object Application {

  implicit class RichRequest(val self: RequestHeader) extends AnyVal {
    def state(id: String): Option[String] = {
      self.headers.get(id).orElse(self.flash.get(id)).orElse(self.session.get(id))
    }
  }

}
