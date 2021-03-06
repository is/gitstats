@file:JvmName("Analyze")
package us.yuxin.gitstats

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Commit(
  val id:String = "",
  val commitTime:Int = 0,
  val interval:Int = 0,
  val author:String = "",
  val parent:String? = null,
  val merge:String? = null,
  val message:String = "",
  val lineAdded:Int = 0,
  val lineModified:Int = 0,
  val lineDeleted:Int = 0,
  val binary:Int = 0,
  val effect:Int = 0,
  var reach:Int = 0,
  var refs:String? = null,
  val changes:List<Change>? = null
)

// @JsonInclude(JsonInclude.Include.NON_DEFAULT)
data class Change(
  val path:String = "",
  val type:Int = -1,
  val section:Int = 0,
  val lineAdded:Int = 0,
  val lineModified:Int = 0,
  val lineDeleted:Int = 0,
  val binary:Boolean = false,
  val effect:Int = 0
)


@JsonPropertyOrder(
  "ts", "id", "date", "time", "refs",
  "author", "ce",
  "path", "section", "effect",
  "added", "modified", "deleted", "binary",
  "month", "year", "day", "hour")
data class ChangeCsv(
  val ts:Int,
  val id:String,
  val date:String,
  val time:String,
  val refs:String,

  val author:String,
  val ce:Int,

  val path:String,
  val section:Int,
  val effect:Int,
  val added:Int,
  val modified:Int,
  val deleted:Int,
  val binary:Int,

  val month:Int,
  val year:Int,
  val day:Int,
  val hour:Int)

data class CommitSet(
  val name:String,
  val repo:GSConfig.Repository,
  val heads:Map<String, String>,
  val branches:List<String>,
  val commits:List<Commit>)


data class CommitSetNullable(
  val name:String? = null,
  val repo:GSConfig.Repository? = null,
  val heads:Map<String, String>? = null,
  val branches:List<String>? = null,
  val commits:List<Commit>? = null) {
  fun toCommitSet():CommitSet {
    return CommitSet(
      name = this.name!!,
      repo = this.repo!!,
      heads = this.heads!!,
      branches = this.branches!!,
      commits = this.commits!!)
  }
}



val csvDateFormatter = DateTimeFormatter.ofPattern("YYYY-MM-dd")
val csvTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
fun localDatetime(timestamp:Long) =
  LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), TimeZone
  .getDefault().toZoneId());
fun localDatetime(timestamp:Int) = localDatetime(timestamp.toLong())

fun changeToCsv(c:Commit, ch:Change):ChangeCsv {
  val time = localDatetime(c.commitTime)

  return ChangeCsv(
    ts = c.commitTime,
    id = c.id,
    date = csvDateFormatter.format(time),
    time = csvTimeFormatter.format(time),
    refs = c.refs?: "-",
    author = c.author,
    ce = c.effect,

    path = ch.path,
    section = ch.section,
    added = ch.lineAdded,
    modified = ch.lineModified,
    deleted = ch.lineDeleted,
    effect = ch.effect,
    binary = if(ch.binary) 1 else 0,

    year = time.year,
    month = time.monthValue,
    day = time.dayOfMonth,
    hour = time.hour)
}


fun changeToCsvs(cs:List<Commit>):List<ChangeCsv> {
  val chs = LinkedList<ChangeCsv>()
  for (c in cs) {
    chs.addAll(c.changes!!.map {changeToCsv(c, it)})
  }
  return chs
}


fun diffText(repo:Repository, diff:DiffEntry):ByteArray {
  val os = ByteArrayOutputStream()
  val formatter = DiffFormatter(os)

  formatter.setRepository(repo)
  formatter.setContext(3)
  formatter.format(diff)
  formatter.close()

  return os.toByteArray()
}


fun analyzeDiff(repo:Repository, diff:DiffEntry):Change {
  var lineAdded:Int = 0
  var lineDeleted:Int = 0
  var lineModified:Int = 0
  var modifyLineAdded:Int = 0
  var modifyLineDeleted:Int = 0
  var section:Int = 0

  var binary = false

  val STATE_INIT = 0
  val STATE_READY = 1

  val LINE_STATE_FIRST = 0
  val LINE_STATE_INLINE = 1
  val LINE_STATE_END = 2

  var lineState:Int = LINE_STATE_FIRST
  var state:Int = STATE_INIT

  val contents = diffText(repo, diff)
  // println("--")
  // println(String(contents))

  val TOKEN_R = '\r'.toByte()
  val TOKEN_N = '\n'.toByte()
  val TOKEN_AT = '@'.toByte()
  val TOKEN_PLUS = '+'.toByte()
  val TOKEN_DASH = '-'.toByte()
  val TOKEN_B = 'B'.toByte()

  var offset:Int = 0
  var cur:Byte;

  // println(String(contents))
  while (offset < contents.size) {
    cur = contents[offset]
    offset += 1

    if (lineState == LINE_STATE_END) {
      if (cur == TOKEN_N || cur == TOKEN_R) {
        continue;
      } else {
        lineState = LINE_STATE_FIRST
      }
    }

    if (lineState == LINE_STATE_FIRST) {
      // first @@ tag
      if (state == STATE_INIT && cur == TOKEN_B) {
        binary = true;
        break;
      }

      lineState = LINE_STATE_INLINE

      if (state == STATE_INIT && cur == TOKEN_AT) {
        state = STATE_READY
        section += 1
        continue
      }

      // Empty line
      if (cur == TOKEN_N || cur == TOKEN_R) {
        lineState = LINE_STATE_END
        continue
      }

      if (state == STATE_INIT) {
        continue;
      }

      if (cur == TOKEN_AT) {
        section += 1

        if (section != 1) {
          lineModified += if (modifyLineAdded > modifyLineDeleted)
            modifyLineAdded else modifyLineDeleted
        }

        modifyLineAdded = 0;
        modifyLineDeleted = 0;
        continue
      }

      if (cur == TOKEN_PLUS) {
        lineAdded += 1
        modifyLineAdded += 1
        continue
      }

      if (cur == TOKEN_DASH) {
        lineDeleted += 1
        modifyLineDeleted += 1
        continue
      }

      if (modifyLineAdded + modifyLineDeleted > 0) {
        lineModified += if (modifyLineAdded < modifyLineDeleted)
          modifyLineAdded else modifyLineDeleted

        modifyLineAdded = 0
        modifyLineDeleted = 0
      }
      continue
    }

    if (lineState != LINE_STATE_FIRST) {
      if (cur == TOKEN_N || cur == TOKEN_R) {
        lineState = LINE_STATE_END
      }
      continue
    }
  }

  if (modifyLineAdded + modifyLineDeleted > 0) {
    lineModified += if (modifyLineAdded < modifyLineDeleted)
      modifyLineAdded else modifyLineDeleted
  }


  val path_ = if (diff.changeType == DiffEntry.ChangeType.DELETE) {
    diff.oldPath
  } else {
    diff.newPath
  }


  return Change(
    path = path_,
    type = diff.changeType.ordinal,
    section = section,
    lineAdded = lineAdded,
    lineModified = lineModified,
    lineDeleted = lineDeleted,
    binary = binary)
}


fun analyzeRev(repo:Repository, rev:RevCommit):Commit {
  val revId = rev.id
  val parents = rev.parents
  val walk = RevWalk(repo)

  val id_ = revId.name
  val time_ = rev.commitTime
  val author_ = rev.authorIdent.emailAddress

  val interval_ = if (parents.size > 0) rev.commitTime - parents[0].commitTime else 0
  val parent_ = if (parents.size > 0) parents[0].id.name else null
  val merge_ = if (parents.size == 2) parents[1].id.name else null

  val objectReader = repo.newObjectReader()

  val newTreeIter = CanonicalTreeParser()
  newTreeIter.reset(objectReader, rev.tree)

  val oldTreeIter = if (parents.size > 0) {
    val parentRev = walk.parseCommit(parents[0])
    val oldTreeIter = CanonicalTreeParser()
    oldTreeIter.reset(objectReader, parentRev.tree)
    oldTreeIter
  } else {
    EmptyTreeIterator()
  }

  val differ = DiffFormatter(null)
  differ.setContext(1)
  differ.setRepository(repo)

  val diffs = differ.scan(oldTreeIter, newTreeIter)
  val changes_ = diffs.map { analyzeDiff(repo, it) }

  val lineAdded_ = changes_.map { it.lineAdded }.sum()
  val lineModified_ = changes_.map { it.lineModified }.sum()
  val lineDeleted_ = changes_.map { it.lineDeleted }.sum()
  val binary_ = changes_.filter{ it.binary }.size

  return Commit(
    id = id_,
    commitTime = time_,
    interval = interval_,
    author = author_,
    parent = parent_,
    merge = merge_,
    message = rev.fullMessage,
    lineAdded = lineAdded_,
    lineDeleted = lineDeleted_,
    lineModified = lineModified_,
    binary = binary_,
    changes = changes_,
    effect = 0,
    reach = 0)
}


fun matchBranches(repo:Repository, branchRules:String?):List<String> {
  val rules = (branchRules?: ".*/master")
    .split(",")
    .map { Regex(it.trim()) }


  val branches = Git(repo)
    .branchList().setListMode(ListBranchCommand.ListMode.ALL)
    .call()
    .map{ it.name.substring(13) }
    .filter(fun(x:String):Boolean {
      for (rule in rules) {
        if (rule.matches(x))
          return true
      }
      return false
    })

  return branches
}



fun analyzeRepository(
  ri:GSConfig.Repository,
  commitCache:MutableMap<String, Commit>? = null):List<Commit> {

  val cache = commitCache ?: HashMap<String, Commit>()

  val repo = ri.repo()
  val git = Git(repo)
  val logs = git.log().all().call()
  val commits = logs.map {
    if (it.name in cache) {
      cache[it.name] as Commit
    } else {
      val commit = analyzeRev(repo, it)
      cache[it.name] = commit
      commit
    }
  }

  return commits
}





