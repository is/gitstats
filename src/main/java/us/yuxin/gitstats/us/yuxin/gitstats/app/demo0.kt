package us.yuxin.gitstats.us.yuxin.gitstats.app

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.util.FS
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


object Clone {
  @JvmStatic
  fun main(args:Array<String>) {
    val sessionFactory = object:JschConfigSessionFactory() {
      override fun configure(hc:OpenSshConfig.Host, session:Session) {
        session.setConfig("StrictHostKeyChecking", "false");
      }

      override fun createDefaultJSch(fs:FS):JSch {
        val con:SSHAgentConnector? =
          if (SSHAgentConnector.isConnectorAvailable()) {
            val usf = JNAUSocketFactory()
            SSHAgentConnector(usf)
          } else {
            null
          }

        return if (con != null) {
          val jsch = JSch()
          jsch.identityRepository = RemoteIdentityRepository(con)
          jsch
        } else {
          super.createDefaultJSch(fs)
        }
      }
    }

    SshSessionFactory.setInstance(sessionFactory)

    val s0:String = "/Users/is/tmp/jsch0.git"
    val s1:String = "/Users/is/tmp/jsch1.git"

    Runtime.getRuntime().exec("rm -fr /Users/is/tmp/jsch0.git")
    Runtime.getRuntime().exec("rm -fr /Users/is/tmp/jsch1.git")

    //    val repo0 = FileRepositoryBuilder()
    //      .setBare().create(File(s0))
    //    val repo1 = FileRepositoryBuilder()
    //      .setBare().create(File(s1))

    var git = Git.cloneRepository()
      .setURI("https://github.com/is/jsch.git")
      .setBare(true)
      .setDirectory(File(s0))
      .setRemote("github")
      .call()
    System.out.println(git.repository)

    // https://gist.github.com/quidryan/5449155


    git = Git.cloneRepository()
      .setURI("git@github.com:is/jsch.git")
      .setBare(true)
      .setDirectory(File(s1))
      .setRemote("github")
      .call()
    System.out.println(git.repository)
  }
}