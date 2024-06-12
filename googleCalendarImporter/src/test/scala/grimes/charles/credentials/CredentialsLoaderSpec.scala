package grimes.charles.credentials

import cats.effect.IO
import cats.effect.kernel.Resource
import com.google.api.services.calendar.CalendarScopes.CALENDAR_EVENTS_READONLY
import com.google.auth.oauth2.GoogleCredentials
import org.http4s.Status.Ok
import org.http4s.client.Client
import org.http4s.dsl.io.*
import org.http4s.{Header, Headers}
import org.typelevel.ci.CIStringSyntax
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import weaver.IOSuite

import java.io.InputStream
import java.nio.charset.StandardCharsets.*
import java.util.UUID
import scala.io.Source

object CredentialsLoaderSpec extends IOSuite {
  override type Res = String

  override def sharedResource: Resource[IO, String] = Resource.fromAutoCloseable(
    IO.blocking(Source.fromURL(getClass.getResource("/ParamStoreResponse.json"), UTF_8.name))
  ).map(_.mkString)

  private val credentialsLoaderStub = new CredentialsLoader[IO] {
    private val googleCredentialsStub = new GoogleCredentials {
      override def refreshIfExpired(): Unit = ()
    }
    
    override protected def buildCredsLoader(credsInputStream: InputStream, scopes: String*): IO[GoogleCredentials] =
      IO.raiseUnless(
        scopes == Seq("https://www.googleapis.com/auth/cloud-platform.read-only", CALENDAR_EVENTS_READONLY)
      )(new RuntimeException("Incorrect scopes"))
        .as(googleCredentialsStub)
  }

  private given logger: Logger[IO] = Slf4jLogger.getLogger

  private val awsSessionToken = UUID.randomUUID().toString
  private val serviceAccountCredsName = "my-service-account"  

  private val expectedHeaders = Headers(
    Header.Raw(ci"X-Aws-Parameters-Secrets-Token", awsSessionToken),
    Header.Raw(ci"Accept", "application/json")
  )
  private val expectedSSMUrl = "http://localhost:2773/systemsmanager/parameters/get?name=my-service-account&withDecryption=true"

  test("Should load access token from service account credentials") { ssmParameterResponse =>
    val stubClient = Client.apply[IO] { request =>
      Resource.eval {
        (
          request.headers == expectedHeaders,
          request.uri.toString == expectedSSMUrl,
        ) match {
          case (false, _) =>
            logger.error(s"Unexpected request headers (${request.headers})") >>
              BadRequest()
          case (true, false) =>
            logger.error(s"Unexpected URL (${request.uri.toString})") >>
              BadRequest()
          case _ => Ok(ssmParameterResponse)
        }
      }
    }

    for {
      result <- credentialsLoaderStub.load(
        serviceAccountCredsName, awsSessionToken, stubClient
      )
    } yield expect.all(
      result.getUniverseDomain == "googleapis.com",
      result.getAuthenticationType == "OAuth2"
    )
  }
}