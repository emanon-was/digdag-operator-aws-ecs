package io.digdag.plugin.aws.ecs.wait


case class WaitConfig(
  val last_run_task: WaitConfig.RunTask
)

object WaitConfig {

  case class RunTask(
    val tasks: List[WaitConfig.Task]
  )

  case class Task(
    val cluster_arn: String,
    val task_arn: String,
    val task_definition_arn: String
  )

}
