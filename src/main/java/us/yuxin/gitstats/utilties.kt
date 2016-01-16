package us.yuxin.gitstats

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.util.FS
import java.io.File

object Utilities {
  @JvmStatic
  fun getGit():Git {
    return Git(getRepository())
  }


  @JvmStatic
  fun getRepository():Repository {
    return FileRepositoryBuilder()
      .setGitDir(File("/Users/is/src/meic2/.git"))
      .build()
  }
}


fun setupJschAgent() {
  val sessionFactory = object:JschConfigSessionFactory() {
    override fun configure(hc:OpenSshConfig.Host, session:Session) {
      session.setConfig("StrictHostKeyChecking", "false");
    }

    override fun createDefaultJSch(fs:FS):JSch {
      val jsch =  if (SSHAgentConnector.isConnectorAvailable()) {
        val usf = JNAUSocketFactory()
        val con = SSHAgentConnector(usf)
        val jsch = JSch()
        jsch.identityRepository = RemoteIdentityRepository(con);
        jsch
      } else {
        super.createDefaultJSch(fs)
      }
      return jsch;
    }
  }

  SshSessionFactory.setInstance(sessionFactory)
}