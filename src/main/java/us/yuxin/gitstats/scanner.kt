package us.yuxin.gitstats

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import org.eclipse.jgit.lib.Repository
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object Scanner {
  @JvmStatic
  fun main(args:Array<String>) {
    setupJschAgent()
    run(args)
  }

  final val C = GSConfig.root()

  fun markCommits(repo:Repository, commits:List<Commit>, branches:List<String>?):Unit {
    val seniorSet = HashSet<String>()
    val commitMap = HashMap<String, Commit>()

    for (commit in commits) {
      commitMap[commit.id] = commit
      if (commit.parent != null)
        seniorSet.add(commit.parent)
      if (commit.merge != null)
        seniorSet.add(commit.merge)
    }


    fun mark(start:String, tag:String) {
      val list = LinkedList<String>()
      list.push(start)

      while(list.size != 0) {
        val id = list.pop()
        val commit = commitMap[id]!!
        if (commit.refs == null) {
          commit.refs = tag
          if (commit.merge != null) {
            list.push(commit.merge)
          }
          if (commit.parent != null) {
            list.push(commit.parent)
          }
        }
      }
    }

    val heads = commits.filter { it.id !in seniorSet }.sortedByDescending { it.commitTime }

    if (branches != null) {
      for (branch in branches) {
        val id = repo.resolve(branch)
        if (id != null) {
          mark(id.name, branch)
        }
      }
    }

    for (head in heads) {
      mark(head.id, "." + head.id.substring(0, 8))
    }
  }


  fun run(@Suppress("UNUSED_PARAMETER") args:Array<String>) {
    val om = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)

    for (repoInfo  in C.repositories) {
      println("  ---")
      println("+ %s: %s".format(repoInfo.name, repoInfo.base(C.workspace)))
      flushRepository(C, repoInfo)
      val commits_ = analyzeRepository(C, repoInfo, null)

      val changes = commits_.map { if(it.changes == null) 0 else it.changes.size } .sum()
      val binarys = commits_.map {
        if(it.changes == null)
          0
        else it.changes.filter { it.binary }.size
      }.sum()



      val repo_ = repoInfo.repo(C)
      val branches_ = matchBranches(repo_, repoInfo.branches)
      markCommits(repo_, commits_, branches_)

      val heads_ = branches_.filter {repo_.resolve(it) != null}
        .toMapBy({it}, {repo_.resolve(it).name})

      val commitSet = CommitSet(
        name = repoInfo.name!!,
        repo = repoInfo,
        heads = heads_,
        branches = branches_,
        commits = commits_)

      val dataPath = Paths.get(C.workspace, "commits", repoInfo.name + ".json")
      Files.createDirectories(dataPath.parent)
      FileWriter(dataPath.toFile()).use {
        om.writeValue(it, commitSet)
      }


      if (C.database) {
        val connection = database()
        saveCommitSetToDatabase(connection, commitSet)
        connection.close()
      }

      val arranger = Arrange(GSConfig.load("arrange.yaml", Arrange.Rules::class.java))
      var commitSet2 = arranger.arrange(commitSet)
      val dataPath2 = Paths.get(C.workspace, "arrange", repoInfo.name + ".json")
      Files.createDirectories(dataPath2.parent)
      FileWriter(dataPath2.toFile()).use {
        om.writeValue(it, commitSet2)
      }

      if (C.database) {
        val connection = database("arrange")
        saveCommitSetToDatabase(connection, commitSet2)
        connection.close()
      }

      val dataPath3 = Paths.get(C.workspace, "arrange", repoInfo.name + ".csv")
      saveCommitSetToCsv(dataPath3.toFile(), commitSet2)

      val lineAdded = commits_.map { it.lineAdded }.sum()
      val lineDeleted = commits_.map { it.lineDeleted }.sum()
      val lineModified = commits_.map { it.lineModified }.sum()
      val lineEffect = commitSet2.commits.map { it.effect }.sum()

      println("  %d/%d commits, %d changes(%d binary), %d (+%d/-%d/%d) lines"
        .format(
          commitSet2.commits.filter { it.merge == null }.size,
          commits_.size, changes, binarys,
          lineEffect, lineAdded, lineDeleted, lineModified
        ))
    }
  }

  fun saveCommitSetToCsv(path:File, cs:CommitSet) {
    val changes = changeToCsvs(cs.commits)
    val cm = CsvMapper()
    val schema = cm.schemaFor(ChangeCsv::class.java).withHeader()
    cm.writer(schema).writeValues(path).writeAll(changes)
  }
}

