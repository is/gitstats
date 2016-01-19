package us.yuxin.gitstats

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Instant
import java.util.*


fun database():Connection {
  val conf = GSConfig.database()
  Class.forName(conf.driver)

  val prop = Properties()
  prop["user"] = conf.user
  prop["password"] = conf.password

  return DriverManager.getConnection(conf.url, prop)
}

/*
data class Commit(
  val id:String = "",
  val time:Int = 0,
  val interval:Int = 0,
  val author:String = "",
  val parent:String? = null,
  val merge:String? = null,
  val message:String = "",
  val lineAdded:Int = 0,
  val lineModified:Int = 0,
  val lineDeleted:Int = 0,
  val binary:Int = 0,
  val effect:Int = 0,
  var refs:String? = null,
  val changes:List<Change>? = null
)
*/

val createCommitsTable = """
CREATE TABLE IF NOT EXISTS commits (
id varchar(80) PRIMARY KEY,
repo varchar(80),
commitTime timestamp,
interval integer,
author varchar(255),
parent varchar(80),
merge varchar(80),
tagline varchar(255),
message text,
added integer,
modified integer,
deleted integer,
binaries INTEGER,
effect INTEGER,
ref varchar(255));
"""


val insertCommits = """
INSERT INTO commits VALUES (
  ?, ?, ?, ?,
  ?, ?, ?, ?,
  ?, ?, ?, ?,
  ?, ?, ?)  ON CONFLICT(id) DO UPDATE SET ref = EXCLUDED.ref
"""

/*
data class Change(
  val path:String = "",
  val type:Int = -1,
  val section:Int = 0,
  val lineAdded:Int = 0,
  val lineModified:Int = 0,
  val lineDeleted:Int = 0,
  val binary:Boolean = false,
  val effect:Int = 0
)
*/


val createChangesTable = """
CREATE TABLE IF NOT EXISTS changes(
id varchar(80),
path varchar(255),
section Integer,
added Integer,
modified Integer,
deleted Integer,
binaries boolean,
effect integer,
PRIMARY KEY(id, path))

"""

val insertChanges = """
INSERT INTO changes VALUES(
  ?, ?, ?, ?,
  ?, ?, ?, ?) ON CONFLICT(id, path) DO NOTHING
"""
/*
val insertChanges = """
INSERT INTO changes VALUES(
  ?, ?, ?, ?,
  ?, ?, ?, ?)
"""
*/


fun saveCommitSetToDatabase(co:Connection, repoName:String, cs:CommitSet):Unit {
  val batchSize = 1
  co.autoCommit = false

  val stmt0 = co.createStatement()

  stmt0.execute(createCommitsTable)
  stmt0.execute(createChangesTable)
  co.commit()
  stmt0.close()

  var cc = 0;

  var stmt = co.prepareStatement(insertCommits)

  for (c in cs.commits) {
    var i = 0
    stmt.setString(++i, c.id)
    stmt.setString(++i, repoName)
    stmt.setTimestamp(++i,
      Timestamp.from(Instant.ofEpochSecond(c.commitTime.toLong())))
    stmt.setInt(++i, c.interval)
    stmt.setString(++i, c.author)
    stmt.setString(++i, c.parent)

    stmt.setString(++i, c.merge)
    stmt.setString(++i, c.message.split("\\n")[0].trim())
    stmt.setString(++i, c.message)
    stmt.setInt(++i, c.lineAdded)
    stmt.setInt(++i, c.lineModified)

    stmt.setInt(++i, c.lineDeleted)
    stmt.setInt(++i, c.binary)
    stmt.setInt(++i, c.effect)
    stmt.setString(++i, c.refs)
    stmt.addBatch()

    cc++
    if (cc > batchSize) {
      stmt.executeBatch()
      co.commit()
      cc = 0
    }
  }

  if (cc > 0) {
    stmt.executeBatch()
    co.commit()
    cc = 0
  }


  stmt = co.prepareStatement(insertChanges)
  for (c in cs.commits) {
    if (c.changes == null) {
      continue
    }

    for (ch in c.changes) {
      var i = 0

      stmt.setString(++i, c.id)
      stmt.setString(++i, ch.path)
      stmt.setInt(++i, ch.section)
      stmt.setInt(++i, ch.lineAdded)
      stmt.setInt(++i, ch.lineModified)

      stmt.setInt(++i, ch.lineDeleted)
      stmt.setBoolean(++i, ch.binary)
      stmt.setInt(++i, ch.effect)

      stmt.addBatch()
      cc++

      if (cc > batchSize) {
        stmt.executeBatch()
        co.commit()
        cc = 0
      }
    }
    if (cc > 0) {
      stmt.executeBatch()
      co.commit()
      cc = 0
    }
  }
}

