package us.yuxin.gitstats

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.TagOpt
import java.io.File
import java.util.*

fun flushRepository(c:GSConfig.Root, repoConfig:GSConfig.Repository) {
  val path = repoConfig.base(c.workspace)
  if (!path.exists()) {
    initRepository(path, repoConfig)
  }
  fetchRepository(path, repoConfig)
}


fun fetchRepository(gitDir:File, repoInfo:GSConfig.Repository) {
  val repo = FileRepositoryBuilder()
    .setBare().setGitDir(gitDir)
    .build()

  val repoConfig = repo.config
  for ((name, url) in repoInfo.remotes.entries) {
    println("  ${name} - ${repoInfo.remote(name)}")
    repoConfig.setString("remote", name, "url", repoInfo.remote(name))
  }
  repoConfig.save()

  val git = Git(repo)
  for (name in repoInfo.remotes.keys) {
    val refSpec = RefSpec("+refs/heads/*:refs/remotes/$name/*")

    val res = git.fetch().setRemote(name)
      .setTagOpt(TagOpt.NO_TAGS)
      .setRefSpecs(refSpec)
      .setRemoveDeletedRefs(true)
      .setCheckFetchedObjects(true).call()

    for (tru in res.trackingRefUpdates) {
      println("  = " + tru.remoteName.substring(11)
        + " - " + tru.localName.substring(13)
        + " : " + tru.oldObjectId.name.substring(0, 8)
        + " -> " + tru.newObjectId.name.substring(0, 8))
    }
  }
}


fun initRepository(gitDir:File, @Suppress("UNUSED_PARAMETER") config:GSConfig.Repository) {
  val parent = gitDir.parentFile
  if (!parent.exists()) {
    parent.mkdirs()
  }
  Git.init().setBare(true).setGitDir(gitDir).call()
}
