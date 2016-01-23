package us.yuxin.gitstats

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
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

class BatchedStatement(val co:Connection, val batchSize:Int) {
  var statement:PreparedStatement? = null
  var count:Int = 0

  fun addBatch() {
    statement!!.addBatch()
    count += 1;

    if (count > batchSize) {
      flush()
    }
  }

  fun flush() {
    if (count != 0) {
      try {
        statement!!.executeBatch()
      } catch(ex:java.sql.BatchUpdateException) {
        println("----")
        println(ex.getNextException().message)
        println("----")
        throw ex;
      }
      co.commit()
    }
    count = 0
  }
}


fun saveCommitSetToDatabase(co:Connection, cs:CommitSet):Unit {
  val batchSize = 1000
  val repoName = cs.name
  co.autoCommit = false

  val stmt0 = co.createStatement()

  stmt0.execute(createCommitsTable)
  stmt0.execute(createChangesTable)
  co.commit()
  stmt0.close()

  val bstmt = BatchedStatement(co, batchSize)
  val stmt1 = co.prepareStatement(insertCommits)
  bstmt.statement = stmt1

  for (c in cs.commits) {
    var i = 0
    stmt1.setString(++i, c.id)
    stmt1.setString(++i, repoName)
    stmt1.setTimestamp(++i,
      Timestamp.from(Instant.ofEpochSecond(c.commitTime.toLong())))
    stmt1.setInt(++i, c.interval)
    stmt1.setString(++i, c.author)
    stmt1.setString(++i, c.parent)

    stmt1.setString(++i, c.merge)
    stmt1.setString(++i, c.message.split("\\n")[0].trim())
    stmt1.setString(++i, c.message)
    stmt1.setInt(++i, c.changes?.size?:0)
    stmt1.setInt(++i, c.lineAdded)
    stmt1.setInt(++i, c.lineModified)

    stmt1.setInt(++i, c.lineDeleted)
    stmt1.setInt(++i, c.binary)
    stmt1.setInt(++i, c.effect)
    stmt1.setInt(++i, c.reach)
    stmt1.setString(++i, c.refs)
    stmt1.setInt(++i, 0)
    bstmt.addBatch()
  }
  bstmt.flush()


  val stmt2 = co.prepareStatement(insertChanges)
  bstmt.statement = stmt2
  for (c in cs.commits) {
    if (c.changes == null) {
      continue
    }

    var serial:Int = 0

    for (ch in c.changes) {
      var i = 0

      stmt2.setString(++i, c.id)
      stmt2.setInt(++i, serial++)
      stmt2.setString(++i, ch.path)
      stmt2.setInt(++i, ch.section)
      stmt2.setInt(++i, ch.lineAdded)

      stmt2.setInt(++i, ch.lineModified)
      stmt2.setInt(++i, ch.lineDeleted)
      stmt2.setBoolean(++i, ch.binary)
      stmt2.setInt(++i, ch.effect)
      stmt2.setInt(++i, 0)

      bstmt.addBatch()
    }
    bstmt.flush()
  }
}

