package us.yuxin.gitstats

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.TagOpt
import java.io.File

fun updateRepo(c:GSConfig.Root, repoConfig:GSConfig.Repository) {
  val path = repoConfig.base(c.workspace)
  println(path)
  if (!path.exists()) {
    initRepo(path, repoConfig)
  }
  fetchRepo(path, repoConfig)
}

fun fetchRepo(gitDir:File, config:GSConfig.Repository) {
  val repo = FileRepositoryBuilder()
    .setBare().setGitDir(gitDir)
    .build()

  // update remote url
  val repoConfig = repo.config
  for ((name, url) in config.remotes.entries) {
    repoConfig.setString("remote", name, "url", url)
    // println(name + ":" + url)
  }
  repoConfig.save()

  val git = Git(repo)
  for (name in config.remotes.keys) {
    // println("-- ${repo}:$name --")
    val refSpec = RefSpec("+refs/heads/*:refs/remotes/$name/*")
    // println(refSpec)

    val res = git.fetch().setRemote(name)
      .setTagOpt(TagOpt.NO_TAGS)
      .setRefSpecs(refSpec)
      .setRemoveDeletedRefs(true)
      .setCheckFetchedObjects(true).call()


    for (tru in res.trackingRefUpdates) {
      println(tru.remoteName + ": " + tru.localName
        + ":" + tru.oldObjectId + " -> " + tru.newObjectId)
    }
  }
}


fun initRepo(gitDir:File, @Suppress("UNUSED_PARAMETER") config:GSConfig.Repository) {
  val parent = gitDir.parentFile
  if (!parent.exists()) {
    parent.mkdirs()
  }
  Git.init().setBare(true).setGitDir(gitDir).call()
}
