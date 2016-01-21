package us.yuxin.gitstats.demo

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.yaml.snakeyaml.Yaml
import us.yuxin.gitstats.*
import java.io.File
import java.io.FileInputStream
import java.util.*


object Log {
  @JvmStatic
  fun main(args:Array<String>) {
    val logs = getDefaultGit().log().call()
    for (rev in logs) {
      println(formatTS(rev.commitTime) + ":" + rev.name + " - " + rev.shortMessage)
    }
  }
}


object Diff {
  @JvmStatic
  fun main(args:Array<String>) {
    val repository = getDefaultRepository()

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


object Clone {
  @JvmStatic
  fun main(args:Array<String>) {
    setupJschAgent()

    val s0:String = "/Users/is/tmp/jsch0.git"
    val s1:String = "/Users/is/tmp/gitstats.git"

    Runtime.getRuntime().exec("rm -fr /Users/is/tmp/jsch0.git")
    Runtime.getRuntime().exec("rm -fr /Users/is/tmp/gitstats.git")

    var git = Git.cloneRepository()
      .setURI("https://github.com/is/jsch.git")
      .setBare(true)
      .setDirectory(File(s0))
      .setRemote("github")
      .call()
    System.out.println(git.repository)


    // https://gist.github.com/quidryan/5449155
    git = Git.cloneRepository()
      .setURI("ssh://git@gitlab.makenv.com:10022/yuxin/gitstats.git")
      .setBare(true)
      .setDirectory(File(s1))
      .setRemote("github")
      .call()
    System.out.println(git.repository)
  }
}


object Yaml0 {
  @JvmStatic
  fun main(args:Array<String>) {
    val yaml = Yaml()
    val conf = FileInputStream("etc/repository.yaml").use { s ->
      yaml.load(s)
    }
    println(conf)
    println()
  }
}


object Config0 {
  @JvmStatic
  fun main(args:Array<String>) {
    val mapper = YAMLMapper()
    mapper.registerKotlinModule()

    val reposType = mapper.typeFactory.constructCollectionLikeType(
      ArrayList::class.java, GSConfig.Repository::class.java)

    val repos = mapper
      .readValue<List<GSConfig.Repository>>(
        File("etc/repos.yaml"), reposType)

    val C = mapper.readValue<GSConfig.Root>(
      File("etc/gitstats.yaml"), GSConfig.Root::class.java)

    val cf = GSConfig.root()

    println(repos)
    println(C)
    println(cf)
  }
}

object Remote0 {
  @JvmStatic
  fun main(args:Array<String>) {
    val repo = FileRepositoryBuilder()
      .setGitDir(File("/Users/is/src/meic2/.git"))
      .build()

    val git = Git(repo)

    for (r in git.lsRemote().call()) {
      println(r)
    }
  }
}


object Diff2{
  @JvmStatic
  fun main(args:Array<String>) {
    val repo = getDefaultRepository()
    val revId = repo.resolve("a20a28ef8fc821dda1dbcdf2f01ecedc291009e4")
    val walk = RevWalk(repo)


    val rev = walk.parseCommit(revId)
    /*
    val rev2 = walk.parseCommit(rev.parents[0])

    val git = Git(repo)
    val objectReader = repo.newObjectReader()
    val newTreeIter = CanonicalTreeParser()
    newTreeIter.reset(objectReader, rev.tree)
    val oldTreeIter = CanonicalTreeParser()
    oldTreeIter.reset(objectReader, rev2.tree)


    val diffs = git.diff()
      .setNewTree(newTreeIter)
      .setOldTree(oldTreeIter)
      .call()

    val change = analyzeDiff(repo, diffs[0])
    println(change)
    */
    val commit = analyzeRev(repo, rev)
    println(commit)
  }
}


object TestPathRule {
  @JvmStatic
  fun main(args:Array<String>) {
    val arrange = Arrange(null)
    val rule = arrange.buildPathRule("1.0002", "-liyoushan@.*")
    println(rule)
    val path = "liyoushan@192.168.1.179/bower_components/jquery-ui/ui/position.js"
    println((rule.data as Regex).matches(path))
  }
}
