package us.yuxin.gitstats

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.jgit.lib.Repository
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

  final val C = GSConfig.load()

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
        if (commit.tags == null) {
          commit.tags = tag
          if (commit.merge != null) {
            list.push(commit.merge)
          }
          if (commit.parent != null) {
            list.push(commit.parent)
          }
        }
      }
    }

    val heads = commits.filter { !(it.id in seniorSet) }.sortedByDescending { it.time }

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

    val om = ObjectMapper()

    for (repo  in C.repositories) {
      println("+ " + repo.name)
      val commits_ = analyzeRepository(C, repo, null)
      println("  " + commits_.size + " commits")

      val branches = repo.branches?: "makenv/master"

      val repo_ = repo.repo(C)
      val branches_ = branches.split(",")
      markCommits(repo_, commits_, branches_)
      // println(commits)
      val heads_ = branches_.filter {repo_.resolve(it) != null}
        .toMapBy({it}, {repo_.resolve(it).name})

      val info = Commits(
        heads = heads_,
        branches = branches_,
        commits = commits_)

      val infoPath = Paths.get(C.workspace, "commits", repo.name + ".json")
      Files.createDirectories(infoPath.parent)
      FileWriter(infoPath.toFile()).use {
        om.writeValue(it, info)
      }
    }
  }


  class Commits(
    var heads:Map<String, String>,
    var branches:List<String>,
    var commits:List<Commit>)
}