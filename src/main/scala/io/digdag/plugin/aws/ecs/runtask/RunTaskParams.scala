package io.digdag.plugin.aws.ecs.runtask

import scala.util.{Try,Failure}
import collection.JavaConverters._
import cats.implicits._
import io.digdag.plugin.aws.ecs.implicits._
import software.amazon.awssdk.services.ecs.model.{
  RunTaskRequest,
  NetworkConfiguration,
  AwsVpcConfiguration,
}
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

  case class Params(
    val params: RunTaskConfig
  )

  def apply(config: Config)(keys: String*): Either[Error, RunTaskParams] =
    for {
      json <- parse(config.toString()).left.map(Error.ConfigJsonParseErr)
      concatJson <- json.flatten(keys: _*).toEither(Error.ConcatJsonErr)
      config <- concatJson.as[Params].left.map(Error.RunTaskConfigParseErr)
      request <- runTaskRequestBuild(config.params).toEither(Error.RunTaskRequestBuildErr)
    } yield RunTaskParams(request)

  def runTaskRequestBuild(config: RunTaskConfig): Try[RunTaskRequest] = {
    for {
      networkConfiguration <- networkConfigurationBuild(config.network_configuration)
    } yield {
      RunTaskRequest.builder()
        .also(builder => config.cluster.map(builder.cluster(_)))
        .also(builder => config.launch_type.map(builder.launchType(_)))
        .also(builder => networkConfiguration.map(builder.networkConfiguration))
        .taskDefinition(config.task_definition)
        .build()
    }
  }

  def networkConfigurationBuild(opt: Option[RunTaskConfig.NetworkConfiguration]): Try[Option[NetworkConfiguration]] = {
    val result = for {
      config <- opt
    } yield for {
      awsvpcConfiguration <- awsvpcConfigurationBuild(config.awsvpc_configuration)
    } yield {
      NetworkConfiguration.builder()
        .also(builder => awsvpcConfiguration.map(builder.awsvpcConfiguration(_)))
        .build()
    }
    result.sequence
  }

  def awsvpcConfigurationBuild(opt: Option[RunTaskConfig.AwsVpcConfiguration]): Try[Option[AwsVpcConfiguration]] = Try{
    for (config <- opt) yield {
      AwsVpcConfiguration.builder()
        .subnets(config.subnets.asJava)
        .also(builder => config.security_groups.map(_.asJava).map(builder.securityGroups(_)))
        .also(builder => config.assign_public_ip.map(builder.assignPublicIp(_)))
        .build()
    }
  }
}
