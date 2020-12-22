package io.digdag.plugin.aws.ecs.runtask

import scala.util.{Try,Failure}
import cats.implicits._
import io.digdag.plugin.aws.ecs.implicits._
import software.amazon.awssdk.services.ecs.{
  EcsClient => SdkEcsClient
}

import software.amazon.awssdk.services.ecs.model.RunTaskRequest
import io.digdag.client.config.Config
import io.circe.parser.parse
import io.circe.generic.auto._

case class RunTaskParams(
  val request: RunTaskRequest,
)

object RunTaskParams {

  sealed trait Error extends Err
  object Error {
    case class ConfigJsonParseErr(val err: Throwable) extends Error with Err.Throws
    case class ConcatJsonErr(val err: Throwable) extends Error with Err.Throws
    case class RunTaskConfigParseErr(val err: Throwable) extends Error with Err.Throws
    case class RunTaskRequestBuildErr(val err: Throwable) extends Error with Err.Throws
  }

  def apply(config: Config)(keys: String*): Either[Error, RunTaskParams] =
    for {
      json <- parse(config.toString()).left.map(Error.ConfigJsonParseErr)
      concatJson <- json.flatten(keys: _*).toEither(Error.ConcatJsonErr)
      config <- concatJson.as[RunTaskConfig].left.map(Error.RunTaskConfigParseErr)
      request <- runTaskRequestBuild(config).toEither(Error.RunTaskRequestBuildErr)
    } yield RunTaskParams(request)

  def runTaskRequestBuild(config: RunTaskConfig): Try[RunTaskRequest] = Failure(new RuntimeException(""))
}
