package us.yuxin.gitstats

import org.eclipse.jgit.api.Git
import java.util.*


data class Commit(
  val id:String,
  val author: String,
  val parents:String,
  val messages:String,
  val lineAdded:Int,
  val lineModified:Int,
  val lineDeleted:Int,
  val binaryAdded:Int,
  val binaryModified:Int,
  val binaryDeleted:Int,
  val merge:Boolean = false,
  val changes:List<Change>
)

data class Change(
  val path:String,
  val section:Int,
  val lineAdded:Int,
  val lineModified:Int,
  val lineDeleted:Int
)


fun analyzeRepository(
  c:GSConfig.Root, config:GSConfig.Repository,
  commitCache:Map<String, Commit>? = null):Unit {

  val cache = commitCache ?: HashMap<String, Commit>()

  val repo = config.repo(c)
  val git = Git(repo)
  println(repo)

  val logc = git.log()
  val branches = config.branches ?: "origin/master"

  for (branch in branches.split(Regex("[\\s,]+"))) {
    logc.add(repo.resolve("refs/remotes/" + branch))
  }
  val logs = logc.call()
  for (rev in logs) {
    println(rev.name + ":" + rev.shortMessage)
  }
}

