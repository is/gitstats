package us.yuxin.gitstats

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

import java.io.File
import java.nio.file.Paths

object GSConfig {
  @JvmStatic
  val CONFIG_FILE = File("etc/gitstats.yaml")

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

    public fun repo(C:Root):org.eclipse.jgit.lib.Repository {
      return FileRepositoryBuilder()
        .setBare()
        .setGitDir(base(C.workspace))
        .build()
    }

    public fun git(C:Root):Git {
      return Git(repo(C))
    }
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
    return load(CONFIG_FILE)
  }
}