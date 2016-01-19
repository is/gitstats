package us.yuxin.gitstats

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File
import java.nio.file.Paths


fun repoRemoteUrl(repo:String):String {
  if (repo.startsWith("https://")
    || repo.startsWith("http://")
    || repo.startsWith("ssh://"))
    return repo

  if (!repo.startsWith("."))
    return repo

  val tokens = repo.substring(1).split("__")
  if (tokens[0] == "makenv") {
    return "ssh://git@gitlab.makenv.com:10022/%s/%s.git".format(tokens[1], tokens[2])
  }
  return repo
}


object GSConfig {
  @JvmStatic
  val CONFIG_FILE = File("etc/gitstats.yaml")
  val CONFIG_FILE_2 = File("conf/gitstats.yaml")
  val CONFIG_FILE_3 = File("gitstats.yaml")

  public data class Root(
    val workspace:String,
    val repositories:List<Repository>)

  public data class Repository(
    val name:String? = null,
    val path:String? = null,
    val remotes:Map<String, String>,
    val branches:String? = null) {

    public val base:String
      get() = path?: name!!

    public fun base(workspace:String):File {
      return Paths.get(workspace, "git", base + ".git").toFile()
    }

    public fun remote(name:String):String {
      return repoRemoteUrl(remotes[name]!!)
    }

    public fun repo(C:Root):org.eclipse.jgit.lib.Repository {
      return FileRepositoryBuilder()
        .setBare()
        .setGitDir(base(C.workspace))
        .build()
    }

    public fun git(C:Root):Git = Git(repo(C))
  }

  @JvmStatic
  fun yamlMapper():YAMLMapper {
    val mapper  = YAMLMapper()
    mapper.registerKotlinModule()
    return mapper
  }

  @JvmStatic
  fun load(path:File):Root {
    return yamlMapper().readValue(path, Root::class.java)
  }

  @JvmStatic
  fun load():Root {
    if (CONFIG_FILE_3.exists()) {
      return load(CONFIG_FILE_3)
    }

    if (CONFIG_FILE_2.exists()) {
      return load(CONFIG_FILE_2)
    }

    return load(CONFIG_FILE)
  }
}