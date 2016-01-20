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
  val CONFIG_PATHS = listOf(".", "conf", "etc")

  fun configPath(confName:String):File? {
    for (cf in CONFIG_PATHS) {
      val fn = File(cf, confName)
      if (fn.exists())
        return fn
    }
    return null
  }

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


  public data class Database(
    val driver:String = "org.postgresql.Driver",
    val url:String? = null,
    val user:String? = null,
    val password:String? = null)

  @JvmStatic
  fun root():Root = load("gitstats.yaml", Root::class.java)
  @JvmStatic
  fun database():Database = load("database.yaml", Database::class.java)


  fun <T> load(cfname:String, clazz:Class<T>):T {
    var c:T? = null
    val ym = yamlMapper()

    for (cp in CONFIG_PATHS) {
      val cf = File(cp, cfname)
      if (!cf.exists()) {
        continue;
      }

      c = if (c == null) {
        ym.readValue(cf, clazz)
      } else {
        ym.readerForUpdating(c).readValue(cf)
      }
    }
    return c!!
  }
}