package us.yuxin.gitstats


object GSConfig {

  public data class Repository(
    val name:String? = null,
    val path:String? = null,
    val remotes:Map<String, String>? = null,
    val branches:List<String>? = null)
}