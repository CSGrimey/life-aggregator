package grimes.charles.common.google.credentials

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.all.*
import com.google.auth.oauth2.GoogleCredentials
import grimes.charles.common.ssm.SSMResponse
import io.circe.*
import io.circe.parser.*
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger

import java.io.{ByteArrayInputStream, InputStream}
import java.nio.charset.StandardCharsets.*
import scala.jdk.CollectionConverters.*

class CredentialsLoader[F[_] : Sync] {
  private def getCreds(ssmResponse: SSMResponse): F[ServiceAccountCreds] = 
    for {
      // The creds are stored in a string that SSM escapes, so have to decode the Value field.
      creds <- Sync[F].fromEither(
        decode[ServiceAccountCreds](ssmResponse.Parameter.Value)
      )
    } yield creds
  
  protected def buildCredsLoader(credsInputStream: InputStream, scopes: NonEmptyList[String]): F[GoogleCredentials] =
    Sync[F].blocking(GoogleCredentials.fromStream(credsInputStream))
      .map(_.createScoped(scopes.toList.asJava))

  private def getAccessToken(credsInputStream: InputStream, scopes: NonEmptyList[String]): F[GoogleCredentials] =
    for {
      googleCreds <- buildCredsLoader(
        credsInputStream,
        scopes
      )
      _ <- Sync[F].blocking(googleCreds.refreshIfExpired())
    } yield googleCreds

  def load(ssmResponse: SSMResponse, scopes: NonEmptyList[String])
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
      accessToken <- getAccessToken(credsInputStream, scopes)
    } yield accessToken

    googleCreds.onError(error =>
      logger.error(s"Failed to load google credentials. Error = $error")
    )
  }
}