package grimes.charles.credentials

import cats.effect.Async
import cats.syntax.all.*
import com.google.api.services.calendar.CalendarScopes.*
import com.google.auth.oauth2.GoogleCredentials
import org.http4s.Method.*
import org.http4s.client.Client
import org.http4s.implicits.uri
import org.http4s.{Header, Headers, Method, Request, Uri}
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets.*

class CredentialsLoader[F[_] : Async] {
  // https://docs.aws.amazon.com/systems-manager/latest/userguide/ps-integration-lambda-extensions.html
  private val credsUrl = uri"http://localhost:2773/systemsmanager/parameters/get"

  private def getCreds(credsName: String, awsSessionToken: String, client: Client[F]): F[String] = {
    val request = Request[F](
      method = GET,
      uri = credsUrl.withQueryParam("name", credsName).withQueryParam("withDecryption", true),
      headers = Headers(
        Header.Raw(ci"X-Aws-Parameters-Secrets-Token", awsSessionToken)
      )
    )

    client.expect[String](request)
  }
  
  protected def buildCredsLoader(credsInputStream: InputStream, scopes: String*): F[GoogleCredentials] = 
    Async[F].blocking(GoogleCredentials.fromStream(credsInputStream))
      .map(_.createScoped(scopes*))

  private def getAccessToken(credsInputStream: InputStream): F[GoogleCredentials] =
    for {
      googleCreds <- buildCredsLoader(
        credsInputStream, 
        "https://www.googleapis.com/auth/cloud-platform.read-only", 
        CALENDAR_EVENTS_READONLY
      )
      _ <- Async[F].blocking(googleCreds.refreshIfExpired())
    } yield googleCreds

  def load(credsName: String, awsSessionToken: String, client: Client[F])
          (using logger: Logger[F]): F[GoogleCredentials] = {
    val googleCreds = for {
      _ <- logger.info("Retrieving google service account credentials")
      credsString <- getCreds(credsName, awsSessionToken, client)
      _ <- logger.info(s"credsString = ${credsString.toString}") // Todo: REMOVE!
      credsInputStream = ByteArrayInputStream(credsString.toString.getBytes(UTF_8.name))

      _ <- logger.info("Using google service account credentials to get access token")
      accessToken <- getAccessToken(credsInputStream)
    } yield accessToken

    googleCreds.onError(error =>
      logger.error(s"Failed to load google credentials. Error = $error")
    )
  }
}