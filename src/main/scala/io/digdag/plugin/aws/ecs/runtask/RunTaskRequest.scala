package io.digdag.plugin.aws.ecs.runtask

import scala.util.Try
import collection.JavaConverters._

import io.digdag.client.config.Config
import io.circe.parser.parse
import io.circe.generic.auto._
import io.digdag.plugin.aws.ecs.implicits._

import software.amazon.awssdk.services.ecs.model.{
  RunTaskRequest => SdkRunTaskRequest,
  NetworkConfiguration,
  AwsVpcConfiguration,
}

object RunTaskRequest {

  sealed trait Error extends Err
  object Error {
    case class ConfigJsonParseErr(val err: Throwable) extends Error with Err.Throws
    case class CascadeJsonErr(val err: Throwable) extends Error with Err.Throws
    case class RunTaskConfigParseErr(val err: Throwable) extends Error with Err.Throws
    case class RunTaskRequestBuildErr(val err: Throwable) extends Error with Err.Throws
  }

  case class Params(
    val params: RunTaskRequestConfig
  )

  def apply(config: Config)(keys: String*): Either[Error, SdkRunTaskRequest] =
    for {
      json <- parse(config.toString()).left.map(Error.ConfigJsonParseErr)
      json <- json.cascade(keys: _*).toEither(Error.CascadeJsonErr)
      config <- json.as[Params].left.map(Error.RunTaskConfigParseErr)
      request <- Try(config.params.build).toEither(Error.RunTaskRequestBuildErr)
    } yield request

  implicit class RunTaskRequestBuild(config: RunTaskRequestConfig) {
    def build: SdkRunTaskRequest =
      SdkRunTaskRequest.builder()
        .optional(config.cluster)((self, value) => self.cluster(value))
        .optional(config.launch_type)((self, value) => self.launchType(value))
        .optional(config.network_configuration)((self, value) => self.networkConfiguration(value.build))
        .required(config.task_definition)((self, value) => self.taskDefinition(value))
        .build()
  }

  implicit class NetworkConfigurationBuild(config: RunTaskRequestConfig.NetworkConfiguration) {
    def build: NetworkConfiguration =
      NetworkConfiguration.builder()
        .optional(config.awsvpc_configuration)((self, value) => self.awsvpcConfiguration(value.build))
        .build()
  }

  implicit class AwsvpcConfigurationBuild(config: RunTaskRequestConfig.AwsVpcConfiguration) {
    def build: AwsVpcConfiguration =
      AwsVpcConfiguration.builder()
        .required(config.subnets)((self, value) => self.subnets(value.asJava))
        .optional(config.security_groups)((self, value) => self.securityGroups(value.asJava))
        .optional(config.assign_public_ip)((self, value) => self.assignPublicIp(value))
        .build()
  }
}
