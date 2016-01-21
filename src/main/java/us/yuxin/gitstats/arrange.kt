package us.yuxin.gitstats

class Arrange(rules:Rules?) {
  data class Rules(
    val path: Map<String, String>?,
    val author: Map<String, String>?,
    val project: Map<String, String>?)

  data class PathRule(
    val id:String,
    val action:Char,
    val type:Int,
    val project:String,
    val rule:String,
    val data:Any?) {
  }

  init {
    setRules(rules)
  }

  var rules:Rules? = null
  var paths:List<PathRule>? = null

  fun setRules(rules:Rules?):Arrange {
    this.rules = rules;
    buildPathRules()
    return this
  }

  val ACTION_SKIP = '!'
  val ACTION_INCLUDE = '+'
  // val ACTION_EXCLUDE = '-'

  val TYPE_NONE = 0
  val TYPE_PATTERN = 1

  fun buildPathRule(id:String, line:String):PathRule {
    if (line[0] == ACTION_SKIP) {
      return PathRule(id, ACTION_SKIP, TYPE_NONE, "*", "NONE", null)
    }

    val action_ = line[0]
    var line_ = line.substring(1)
    val project_ = if (line_.indexOf(":::") == -1) {
      "*"
    } else {
      val tokens = line_.split(Regex(":::"), 2)
      line_ = tokens[1]
      tokens[0]
    }
    return PathRule(id, action_, TYPE_PATTERN, project_, line_, Regex(line_))
  }

  fun buildPathRules() {
    if (this.rules?.path == null) {
      paths = null
      return
    }

    val keys = this.rules?.path!!.keys.sorted()
    paths = keys.map { buildPathRule(it, this.rules?.path!![it]!!) }
  }


  fun isPathIncluded(path:String, project:String = "unset"):Boolean {
    if (paths == null) {
      return true
    }

    for (pathRule in paths!!) {
      if (pathRule.action == ACTION_SKIP) {
        continue
      }

      if (pathRule.project != "*" && pathRule.project.compareTo(project) != 0) {
        continue
      }

      if (pathRule.type == TYPE_PATTERN &&
        (pathRule.data as Regex?)!!.matches(path)) {
        return if(pathRule.action == ACTION_INCLUDE) true else false
      }
    }
    return true
  }

  fun mapAuthor(name:String):String {
    return if (rules?.author == null) {
      name
    } else {
      rules!!.author!![name]?:name
    }
  }


  fun arrangeCommit(ri:GSConfig.Repository, c:Commit):Commit {
    if (c.merge != null) {
      return c.copy(author = mapAuthor(c.author))
    }

    val changes_= c.changes!!.map({
      ch ->
      if (ch.binary || ch.effect != 0
        || ch.lineAdded + ch.lineDeleted > 800
        || !isPathIncluded(ch.path, ri.name!!)) {
        ch
      } else {
        ch.copy(effect = ch.lineAdded)
      }
    })

    return c.copy(
      author = mapAuthor(c.author),
      effect = changes_.map { it.effect }.sum(),
      changes = changes_
    )
  }


  public fun arrange(cs:CommitSet):CommitSet {
    return cs.copy(
      commits = cs.commits.map {
        arrangeCommit(cs.repo, it)
      })
  }
}
