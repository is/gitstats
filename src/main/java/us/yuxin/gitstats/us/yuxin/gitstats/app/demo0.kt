package us.yuxin.gitstats.us.yuxin.gitstats.app

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.File


fun getGit():Git {
  return Git(getRepository())
}

fun getRepository():Repository {
  return FileRepositoryBuilder()
    .setGitDir(File("/Users/is/src/meic2/.git"))
    .build()
}


object Log {
  @JvmStatic
  fun main(args:Array<String>) {
    val logs = getGit().log().call()
    for (rev in logs) {
      println(rev.name + " - " + rev.shortMessage)
    }
  }
}


object Diff {
  @JvmStatic
  fun main(args:Array<String>) {
    val repository = getRepository()

    val rid0 = repository.resolve("178958f5de")
    val rid1 = repository.resolve("035aaf1e1d")

    println(rid0)
    println(rid1)

    val walk = RevWalk(repository)
    val rev1 = walk.parseCommit(rid1)
    val rev0 = walk.parseCommit(rid0)

    println(rev0)
    println(rev1)

    val reader = repository.newObjectReader()

    val oldTreeIter = CanonicalTreeParser()
    oldTreeIter.reset(reader, rev1.tree)
    val newTreeIter = CanonicalTreeParser()
    newTreeIter.reset(reader, rev0.tree)

    val git = Git(repository)
    val diffs = git.diff()
      .setNewTree(newTreeIter)
      .setOldTree(oldTreeIter)
      .call()

    val diffFormatter = DiffFormatter(System.out)
    diffFormatter.setRepository(repository)
    diffFormatter.setContext(1)

    for (e in diffs) {
      println("entry:" + e)
      println(diffFormatter.format(e))
    }
  }
}