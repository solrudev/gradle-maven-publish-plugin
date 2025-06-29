package com.vanniktech.maven.publish.sonatype

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkQueue
import org.gradle.workers.WorkerExecutor

internal abstract class CreateSonatypeRepositoryTask : DefaultTask() {
  @get:Internal
  abstract val projectGroup: Property<String>

  @get:Input
  abstract val artifactId: Property<String>

  @get:Input
  abstract val version: Property<String>

  @get:Internal
  abstract val buildService: Property<SonatypeRepositoryBuildService>

  @Inject
  abstract fun getWorkerExecutor(): WorkerExecutor

  @TaskAction
  fun createStagingRepository() {
    val workQueue: WorkQueue = getWorkerExecutor().noIsolation()
    workQueue.submit(CreateStagingRepository::class.java) {
      requireNotNull(it)
      it.group.set(projectGroup)
      it.artifactId.set(artifactId)
      it.version.set(version)
      it.buildService.set(buildService)
    }
  }

  internal interface CreateStagingRepositoryParameters : WorkParameters {
    val buildService: Property<SonatypeRepositoryBuildService>
    val group: Property<String>
    val artifactId: Property<String>
    val version: Property<String>
  }

  abstract class CreateStagingRepository : WorkAction<CreateStagingRepositoryParameters?> {
    override fun execute() {
      val parameters = requireNotNull(parameters)
      val service = parameters.buildService.get()
      val group = parameters.group.get()
      val artifactId = parameters.artifactId.get()
      val version = parameters.version.get()
      service.createStagingRepository(group, artifactId, version)
    }
  }

  companion object {
    private const val NAME = "createStagingRepository"

    fun TaskContainer.registerCreateRepository(
      buildService: Provider<SonatypeRepositoryBuildService>,
      group: Provider<String>,
      artifactId: Provider<String>,
      version: Provider<String>,
    ): TaskProvider<CreateSonatypeRepositoryTask> = register(NAME, CreateSonatypeRepositoryTask::class.java) {
      it.description = "Create a staging repository on Sonatype OSS"
      it.group = "release"
      it.projectGroup.set(group)
      it.artifactId.set(artifactId)
      it.version.set(version)
      it.buildService.set(buildService)
      it.usesService(buildService)
    }
  }
}
