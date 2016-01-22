package us.yuxin.gitstats

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.stringtemplate.v4.ST
import java.io.File
import java.nio.file.Paths

object GSConfig {
  val CONFIG_PATHS = listOf("etc", "conf", "this", "this/etc", ".")


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
    val database:Boolean = false,
    val useCommitCache:Boolean = true,
    val threads:Int = 1,
    val repositories:List<Repository>,
    val remoteTemplates:Map<String, String>? = null) {

    fun remote(repo:String):String {
      if (remoteTemplates == null)
        return repo

      if (repo.startsWith("https://")
        || repo.startsWith("http://")
        || repo.startsWith("ssh://"))
        return repo

      if (!repo.startsWith("."))
        return repo

      val tokens = repo.substring(1).split("__")
      if (tokens[0] in remoteTemplates) {
        val st = ST(remoteTemplates[tokens[0]])

        for (i in 0..tokens.size - 1) {
          st.add("p" + i, tokens[i])
        }
        return st.render()
      }

      return repo
    }
  }

  public data class Repository(
    val name:String? = null,
    val path:String? = null,
    val remotes:Map<String, String>,
    val branches:String? = null) {

    @JsonIgnore
    var root:Root? = null;

    val base:String
    @JsonIgnore get() = path?: name!!

    val gitDir:File
    @JsonIgnore get() = Paths.get(root!!.workspace, "git", base + ".git").toFile()

    public fun remote(name:String) = root!!.remote(remotes[name]!!)

    public fun repo() =
      FileRepositoryBuilder()
        .setBare()
        .setGitDir(gitDir)
        .build()

    public fun git():Git = Git(repo())
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
    val ym = yamlMapper()
    var root:JsonNode? = null

    for (cp in CONFIG_PATHS) {
      val cf = File(cp, cfname)
      if (!cf.exists()) {
        continue;
      }

      root = merge(root, ym.readTree(cf))
    }
    return ym.treeToValue(root as TreeNode, clazz)
  }


  @JvmStatic
  fun merge(mainNode:JsonNode?, updateNode:JsonNode):JsonNode {
    if (mainNode == null) {
      return updateNode
    }

    for (updatedFieldName in updateNode.fieldNames()) {
      val valueToBeUpdated = mainNode[updatedFieldName]
      val updatedValue = updateNode[updatedFieldName]

      if (valueToBeUpdated != null && updatedValue.isArray) {
        for (i in 0..updatedValue.size() - 1) {
          val updatedChildNode = updatedValue[i]
          if (valueToBeUpdated.size() <= i) {
            (valueToBeUpdated as ArrayNode).add(updatedChildNode)
          }
          merge(valueToBeUpdated[i], updatedChildNode)
        }
      } else if (valueToBeUpdated != null && valueToBeUpdated.isObject) {
        merge(valueToBeUpdated, updatedValue)
      } else {
        if (mainNode is ObjectNode) {
          mainNode.replace(updatedFieldName, updatedValue)
        }
      }
    }
    return mainNode
  }
}