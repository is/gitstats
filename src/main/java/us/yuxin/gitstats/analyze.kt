package us.yuxin.gitstats

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.util.*


data class Commit(
  val id:String,
  val author: String,
  val parent:String,
  val merge:String?,
  val message:String,
  val lineAdded:Int,
  val lineModified:Int,
  val lineDeleted:Int,
  val binaryAdded:Int,
  val binaryModified:Int,
  val binaryDeleted:Int,
  val changes:List<Change>?
)


data class Change(
  val path:String,
  val type:Int,
  val section:Int,
  val lineAdded:Int,
  val lineModified:Int,
  val lineDeleted:Int
)

private fun analyzeDiff(repo:Repository, diff:DiffEntry):Change {
  return Change(
    path = diff.newPath,
    type = diff.changeType.ordinal,
    section = 0,
    lineAdded = 0,
    lineModified = 0,
    lineDeleted = 0)
}


fun analyzeRev(repo:Repository, rev:RevCommit):Commit {
  val git = Git(repo)
  val revId = rev.id
  val parents = rev.parents

  val id_ = revId.toString()
  val parent_ = parents[0].id.toString()
  val author_ = rev.authorIdent.emailAddress

  val merge_ = if (parents.size == 2) {
    parents[1].id.toObjectId().toString()
  } else {
    null
  }

  val objectReader = repo.newObjectReader()

  val newTreeIter = CanonicalTreeParser()
  newTreeIter.reset(objectReader, rev.tree)
  val oldTreeIter = CanonicalTreeParser()
  oldTreeIter.reset(objectReader, rev.parents[0].tree)

  val diffs = git.diff()
    .setNewTree(newTreeIter)
    .setOldTree(oldTreeIter)
    .call()

  val changes_ = diffs.map {analyzeDiff(repo, it)}

  return Commit(
    id = id_,
    author = author_,
    parent = parent_,
    merge = merge_,
    message = rev.fullMessage,
    lineAdded = 0,
    lineDeleted = 0,
    lineModified = 0,
    binaryAdded = 0,
    binaryDeleted = 0,
    binaryModified = 0,
    changes = changes_)
}


fun analyzeRepository(
  c:GSConfig.Root, config:GSConfig.Repository,
  commitCache:MutableMap<String, Commit>? = null):Unit {

  val cache = commitCache ?: HashMap<String, Commit>()

  val repo = config.repo(c)
  val git = Git(repo)
  println(repo)

  val logc = git.log()

  val logs = logc.all().call()
  for (rev in logs) {
    if (rev.id.toString() in cache) {
      continue
    }

    val commit = analyzeRev(repo, rev)
    cache[commit.id] = commit
  }
}

