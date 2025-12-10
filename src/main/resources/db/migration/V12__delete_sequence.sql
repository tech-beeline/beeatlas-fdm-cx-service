
ALTER TABLE cx.bi_steps_relations
    ALTER COLUMN id DROP DEFAULT;

DROP SEQUENCE IF EXISTS cx.bi_steps_relations_id_seq;
