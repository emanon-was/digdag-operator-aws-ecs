package io.digdag.plugin.aws.ecs.client

import scala.util.Try
import io.digdag.plugin.aws.ecs.implicits._
import software.amazon.awssdk.services.ecs.{
  EcsClient => SdkEcsClient
}
import io.digdag.client.config.Config

object EcsClient {

  sealed trait Error extends Err
  object Error {
    case class AwsClientParamsErr(val err: Err) extends Error with Err.Enum
    case class ClientBuildErr(val err: Throwable) extends Error with Err.Throws
  }

  def apply(config: Config)(keys: String*): Either[Error, SdkEcsClient] =
    for {
      params <- AwsClientParams(config)(keys: _*).left.map(Error.AwsClientParamsErr)
      client <- clientBuild(params).toEither(Error.ClientBuildErr)
    } yield client

  def clientBuild(params: AwsClientParams): Try[SdkEcsClient] = Try {
    SdkEcsClient.builder()
      .also(builder => params.credentialsProvider.map(builder.credentialsProvider(_)))
      .also(builder => params.region.map(builder.region(_)))
      .build()
  }
}
