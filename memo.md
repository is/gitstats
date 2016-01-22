
https://github.com/centic9/jgit-cookbook


http://www.jooq.org/doc/3.7/manual/code-generation/codegen-gradle/
https://github.com/jOOQ/jOOQ/tree/master/jOOQ-examples/jOOQ-codegen-gradle

===

java -Xmx4G -cp $(cat __cp) us.yuxin.gitstats.Scanner


===
CREATE VIEW arrange.cs AS
SELECT
c.id as id, c.committime, 
c.author, c.changed, c.interval,
c.message, c.tagline,
c.parent, c.merge,
c.ref, c.reach,
c.effect as ceffect,
c.added as cadded, c.modified as cmodified,
c.deleted as cdeleted,
c.binaries as cbinaries,
h.path, h.section, h.effect,
h.added, h.modified, h.deleted, h.binaries
FROM  changes as h LEFT JOIN commits as c ON (c.id = h.id);
