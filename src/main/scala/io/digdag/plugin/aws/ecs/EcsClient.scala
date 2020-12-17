package io.digdag.plugin.aws.ecs.runtask

import scala.util.Try
import io.digdag.plugin.aws.ecs.implicits._
import software.amazon.awssdk.services.ecs.{
  EcsClient => SdkEcsClient
}

object EcsClient {

  sealed trait Error extends Err
  object Error {
    case class ClientBuildErr(val err: Throwable) extends Error with Err.Throws
  }

  def apply(params: EcsClientParams): Either[Error, SdkEcsClient] = {
    val clientBuild = Try {
      SdkEcsClient.builder()
        .also(builder => params.credentialsProvider.map(builder.credentialsProvider(_)))
        .also(builder => params.region.map(builder.region(_)))
        .build()
    }
    clientBuild.toEither(Error.ClientBuildErr)
  }
}
