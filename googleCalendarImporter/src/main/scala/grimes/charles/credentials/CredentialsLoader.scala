package grimes.charles.credentials

import cats.effect.Async
import cats.syntax.all.*
import com.google.api.services.calendar.CalendarScopes.*
import com.google.auth.oauth2.GoogleCredentials
import org.apache.logging.log4j.Logger
import org.http4s.Method.*
import org.http4s.client.Client
import org.http4s.implicits.uri
import org.http4s.{Header, Headers, Method, Request, Uri}
import org.typelevel.ci.CIStringSyntax

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.*

object CredentialsLoader {
  private def getServiceAccountCreds[F[_] : Async](serviceAccountCredsName: String, awsSessionToken: String, client: Client[F]): F[String] = {
    // https://docs.aws.amazon.com/systems-manager/latest/userguide/ps-integration-lambda-extensions.html
    val credentialsUrl = uri"http://localhost:2773/systemsmanager/parameters/get/" 
       
    val request = Request[F](
      method = GET,
      uri = credentialsUrl.withQueryParam("name", serviceAccountCredsName),
      headers = Headers(
        Header.Raw(ci"X-Aws-Parameters-Secrets-Token", awsSessionToken)
      )
    )

    client.expect[String](request)
  }

  def load[F[_]: Async](serviceAccountCredsName: String, awsSessionToken: String, client: Client[F])
                      (using logger: Logger): F[GoogleCredentials] = {
    val googleCreds = for {
      _ <- Async[F].delay(logger.info("Retrieve google service account credentials"))
      serviceAccountCredsString <- getServiceAccountCreds(
        serviceAccountCredsName, awsSessionToken, client
      )
      serviceAccountCredsInputStream = ByteArrayInputStream(
        serviceAccountCredsString.getBytes(UTF_8.name)
      )

      _ <- Async[F].delay(logger.info("Using google service account credentials to get access token"))
      googleCreds <- Async[F].blocking(
        GoogleCredentials
        .fromStream(serviceAccountCredsInputStream)
        .createScoped(
          "https://www.googleapis.com/auth/cloud-platform.read-only",
          CALENDAR_EVENTS_READONLY
        )
      )
      _ <- Async[F].blocking(googleCreds.refreshIfExpired())
    } yield googleCreds

    googleCreds.onError(error =>
      Async[F].delay(logger.error(s"Failed to load google credentials. Error = $error"))
    )
  }
}