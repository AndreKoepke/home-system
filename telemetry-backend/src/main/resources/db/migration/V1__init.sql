CREATE TABLE telemetry_record
(
  id              uuid PRIMARY KEY,
  last_contact    timestamp NOT NULL,
  git_branch      text,
  git_commit      text,
  git_commit_date timestamp,
  contacts        integer
);
