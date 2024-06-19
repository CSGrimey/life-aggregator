package grimes.charles.credentials

import cats.effect.Sync
import cats.syntax.all.*
import com.google.api.services.calendar.CalendarScopes.*
import com.google.auth.oauth2.GoogleCredentials
import grimes.charles.common.ssm.SSMResponse
import io.circe.*
import io.circe.parser.*
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets.*

class CredentialsLoader[F[_] : Sync] {
  private def getCreds(ssmResponse: SSMResponse): F[ServiceAccountCreds] = 
    for {
      // The creds are stored in a string that SSM escapes, so have to decode the Value field.
      creds <- Sync[F].fromEither(
        decode[ServiceAccountCreds](ssmResponse.Parameter.Value)
      )
    } yield creds
  
  protected def buildCredsLoader(credsInputStream: InputStream, scopes: String*): F[GoogleCredentials] =
    Sync[F].blocking(GoogleCredentials.fromStream(credsInputStream))
      .map(_.createScoped(scopes*))

  private def getAccessToken(credsInputStream: InputStream): F[GoogleCredentials] =
    for {
      googleCreds <- buildCredsLoader(
        credsInputStream, 
        "https://www.googleapis.com/auth/cloud-platform.read-only", 
        CALENDAR_EVENTS_READONLY
      )
      _ <- Sync[F].blocking(googleCreds.refreshIfExpired())
    } yield googleCreds

  def load(ssmResponse: SSMResponse)
          (using logger: Logger[F]): F[GoogleCredentials] = {
    val googleCreds = for {
      _ <- logger.info("Retrieving google service account credentials")
      creds <- getCreds(ssmResponse)
      credsInputStream <- Sync[F].delay(
        ByteArrayInputStream(
          ServiceAccountCreds
            .encoder
            .apply(creds)
            .toString
            .getBytes(UTF_8.name)
        )
      )

      _ <- logger.info("Using google service account credentials to get access token")
      accessToken <- getAccessToken(credsInputStream)
    } yield accessToken

    googleCreds.onError(error =>
      logger.error(s"Failed to load google credentials. Error = $error")
    )
  }
}