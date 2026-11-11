-- Extensions required by the schema.
-- btree_gist backs the exclusion constraint that prevents doctor double-booking at the DB level.
create extension if not exists pgcrypto;
create extension if not exists btree_gist;
