# Improvements

Changes to behavior that S00–S03 already define. Each one becomes its own spec (`I0N-*.md`) before it
is implemented; that spec then supersedes the rule it changes, and the changed spec gets a
"Superseded by I0N" note.

## Candidates (not specified, do NOT implement)

| # | Current behavior | Where | Possible improvement |
|---|---|---|---|
| 1 | The `feeds` collection has only the `_id` index; the front page and deduplication scan it | S01/S02/S03 | Indexes on `source + createdAt` and `source + title + createdAt` (Python port I08), created by a versioned migration (Python I07) |
| 2 | The CMS routes do not exist | S00 | Port `PUT`, `GET`, `PATCH`, `DELETE /feed/{id}` and `GET /feed/list` (Python S02–S05 with I01, I06, I08) |
| 3 | Single-flight guard and cooldown live in the process | S03 | A distributed lock (Mongo or a queue such as SQS) for several instances |
| 4 | The refresh is triggered by reads | S03 | A scheduled job |
| 5 | El País is not scraped | S02 | Restore it if it becomes reachable without a browser |
