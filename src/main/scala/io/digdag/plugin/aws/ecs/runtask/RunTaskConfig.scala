package io.digdag.plugin.aws.ecs.runtask

case class RunTaskConfig(
  val cluster: String,
  val task_definition: String,
  val launch_type: String,
  val network_configuration: RunTaskConfig.NetworkConfiguration
)

object RunTaskConfig {
  case class NetworkConfiguration(
    val awsvpc_configuration: AwsVpcConfiguration
  )
  case class AwsVpcConfiguration(
    val subnets: List[String],
    val security_groups: List[String],
    val assign_public_ip: String
  )
}



