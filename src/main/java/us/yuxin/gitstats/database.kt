package us.yuxin.gitstats

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Instant
import java.util.*


fun database(schema:String? = null):Connection {
  val conf = GSConfig.database()
  Class.forName(conf.driver)

  val prop = Properties()
  prop["user"] = conf.user
  prop["password"] = conf.password
  if (schema != null) {
    prop["currentSchema"] = "$schema,\"$\\user\",public"
  }
  return DriverManager.getConnection(conf.url, prop)
}


val createCommitsTable = """
CREATE TABLE IF NOT EXISTS commits (
id VARCHAR(80) PRIMARY KEY,
repo VARCHAR(80),
commitTime timestamp,
interval INTEGER,
author VARCHAR(255),
parent VARCHAR(80),
merge VARCHAR(80),
tagline VARCHAR(255),
message text,
changed INTEGER,
added INTEGER,
modified INTEGER,
deleted INTEGER,
binaries INTEGER,
effect INTEGER,
reach INTEGER,
ref VARCHAR(255),
stick INTEGER);
"""


val insertCommits = """
INSERT INTO commits VALUES (
  ?, ?, ?, ?,
  ?, ?, ?, ?,
  ?, ?, ?, ?,
  ?, ?, ?, ?,
  ?, ?)  ON CONFLICT(id)
  DO UPDATE SET
  ref = EXCLUDED.ref, author = EXCLUDED.author,
  effect = EXCLUDED.effect, reach = EXCLUDED.reach
"""

val createChangesTable = """
CREATE TABLE IF NOT EXISTS changes(
id VARCHAR(80),
serial INTEGER,
path VARCHAR(255),
section INTEGER,
added INTEGER,
modified INTEGER,
deleted INTEGER,
binaries BOOLEAN,
effect INTEGER,
stick INTEGER,
PRIMARY KEY(id, path))
"""

val insertChanges = """
INSERT INTO changes VALUES(
  ?, ?, ?, ?, ?,
  ?, ?, ?, ?, ?) ON CONFLICT(id, path)
  DO UPDATE SET effect = EXCLUDED.effect
"""
/*
val insertChanges = """
INSERT INTO changes VALUES(
  ?, ?, ?, ?,
  ?, ?, ?, ?)
"""
*/


fun saveCommitSetToDatabase(co:Connection, cs:CommitSet):Unit {
  val batchSize = 1000
  val repoName = cs.name
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
    stmt.setInt(++i, c.changes?.size?:0)
    stmt.setInt(++i, c.lineAdded)
    stmt.setInt(++i, c.lineModified)

    stmt.setInt(++i, c.lineDeleted)
    stmt.setInt(++i, c.binary)
    stmt.setInt(++i, c.effect)
    stmt.setInt(++i, c.reach)
    stmt.setString(++i, c.refs)
    stmt.setInt(++i, 0)
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

    var serial:Int = 0

    for (ch in c.changes) {
      var i = 0

      stmt.setString(++i, c.id)
      stmt.setInt(++i, serial++)
      stmt.setString(++i, ch.path)
      stmt.setInt(++i, ch.section)
      stmt.setInt(++i, ch.lineAdded)

      stmt.setInt(++i, ch.lineModified)
      stmt.setInt(++i, ch.lineDeleted)
      stmt.setBoolean(++i, ch.binary)
      stmt.setInt(++i, ch.effect)
      stmt.setInt(++i, 0)

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

