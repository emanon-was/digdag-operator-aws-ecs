package io.digdag.plugin.aws.ecs.runtask

case class RunTaskConfig(
  // val capacity_provider_strategy: Option[List[RunTaskConfig.CapacityProviderStrategy]],
  val cluster: Option[String],
  // val count: Option[Int],
  val enable_ecs_managed_tags: Option[Boolean],
  val group: Option[String],
  val launch_type: Option[String],
  val network_configuration: Option[RunTaskConfig.NetworkConfiguration],
  val overrides: Option[RunTaskConfig.Override],
  // val placement_constraints: Option[List[RunTaskConfig.PlacementConstraint]],
  // val placement_strategy: Option[List[RunTaskConfig.PlacementStrategy]],
  val platform_version: Option[String],
  // val propagate_tags: Option[String],
  // val reference_id: Option[String],
  // val started_by: Option[String],
  val tags: Option[List[RunTaskConfig.Tag]],
  val task_definition: String,
)

object RunTaskConfig {

  // CapacityProviderStrategy

  case class CapacityProviderStrategy(
    val capacityProvider: String,
    val weight: Option[Integer],
    val base: Option[Integer]
  )

  // NetworkConfiguration

  case class NetworkConfiguration(
    val awsvpc_configuration: Option[AwsVpcConfiguration]
  )

  case class AwsVpcConfiguration(
    val subnets: List[String],
    val security_groups: Option[List[String]],
    val assign_public_ip: Option[String]
  )

  // Override

  case class Override(
    val containerOverrides: Option[List[ContainerOverride]],
    val cpu: Option[String],
    val inference_accelerator_overrides: Option[List[InferenceAcceleratorOverride]],
    val execution_role_arn: Option[String],
    val memory: Option[String],
    val task_role_arn: Option[String],
  )

  case class ContainerOverride(
    val name: Option[String],
    val command: Option[List[String]],
    val environment: Option[List[Environment]],
    val environment_files: Option[List[EnvironmentFile]],
    val cpu: Option[Integer],
    val memory: Option[Integer],
    val memory_reservation: Option[Integer],
    val resource_requirements: Option[List[ResourceRequirement]],
  )

  case class Environment(
    val name: String,
    val value: String,
  )

  case class EnvironmentFile(
    val value: String,
    val `type`: String,
  )

  case class ResourceRequirement(
    val value: String,
    val `type`: String,
  )

  case class InferenceAcceleratorOverride(
    val device_name: Option[String],
    val device_type: Option[String],
  )

  // PlacementConstraint

  case class PlacementConstraint(
    val `type`: String,
    val expression: String,
  )

  // PlacementStrategy

  case class PlacementStrategy(
    val `type`: String,
    val field: String,
  )

  // Tag

  case class Tag(
    val key: String,
    val value: String,
  )
}
