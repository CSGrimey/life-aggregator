package grimes.charles.common.ssm

import cats.effect.IO
import cats.effect.kernel.Resource
import org.http4s.dsl.io.*
import org.http4s.client.Client
import org.http4s.{Header, Headers}
import weaver.IOSuite
import org.typelevel.log4cats.SelfAwareStructuredLogger as Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.ci.CIStringSyntax
import java.nio.charset.StandardCharsets.*

import java.util.UUID
import scala.io.Source

object ParamsStoreSpec extends IOSuite {
  override type Res = String

  override def sharedResource: Resource[IO, String] = Resource.fromAutoCloseable(
    IO.blocking(Source.fromURL(getClass.getResource("/SimpleParam.json"), UTF_8.name))
  ).map(_.mkString)
  
  private given logger: Logger[IO] = Slf4jLogger.getLogger

  private val awsSessionToken = UUID.randomUUID().toString
  private val ssmParamName = "my-ssm-param"

  private val expectedHeaders = Headers(
    Header.Raw(ci"X-Aws-Parameters-Secrets-Token", awsSessionToken),
    Header.Raw(ci"Accept", "application/json")
  )
  private val expectedSSMUrl = "http://localhost:2773/systemsmanager/parameters/get?name=my-ssm-param&withDecryption=true"

  test("Should load SSM param") { ssmParameterResponse =>
    val clientStub = Client.apply[IO] { request =>
      Resource.eval {
        (
          request.headers == expectedHeaders,
          request.uri.toString == expectedSSMUrl
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
      result <- ParamsStore[IO].get(
        ssmParamName, awsSessionToken, clientStub
      )
    } yield expect(result.Parameter.Value == "Some value from params store")
  }
}
