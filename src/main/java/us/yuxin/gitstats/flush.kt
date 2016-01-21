package us.yuxin.gitstats

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.TagOpt
import java.io.File

fun flushRepository(ri:GSConfig.Repository) {
  val path = ri.gitDir
  if (!path.exists()) {
    initRepository(ri)
  }
  fetchRepository(ri)
}


fun fetchRepository(ri:GSConfig.Repository) {
  val repo = FileRepositoryBuilder()
    .setBare().setGitDir(ri.gitDir)
    .build()

  val repoConfig = repo.config
  for ((name, url) in ri.remotes.entries) {
    println("  ${name} - ${ri.remote(name)}")
    repoConfig.setString("remote", name, "url", ri.remote(name))
  }
  repoConfig.save()

  val git = Git(repo)
  for (name in ri.remotes.keys) {
    val refSpec = RefSpec("+refs/heads/*:refs/remotes/$name/*")

    val res = git.fetch().setRemote(name)
      .setTagOpt(TagOpt.NO_TAGS)
      .setRefSpecs(refSpec)
      .setRemoveDeletedRefs(true)
      .setCheckFetchedObjects(true).call()

    for (tru in res.trackingRefUpdates) {
      println("  > " + tru.remoteName.substring(11)
        + " - " + tru.localName.substring(13)
        + " : " + tru.oldObjectId.name.substring(0, 8)
        + " to " + tru.newObjectId.name.substring(0, 8))
    }
  }
}


fun initRepository(ri:GSConfig.Repository) {
  val parent = ri.gitDir.parentFile
  if (!parent.exists()) {
    parent.mkdirs()
  }
  Git.init().setBare(true).setGitDir(ri.gitDir).call()
}
