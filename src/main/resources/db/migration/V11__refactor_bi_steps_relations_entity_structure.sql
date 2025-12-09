
ALTER TABLE cx.bi_steps_relations ADD COLUMN IF NOT EXISTS description text NULL;
ALTER TABLE cx.bi_steps_relations ADD COLUMN IF NOT EXISTS product_id int4 NULL;
ALTER TABLE cx.bi_steps_relations ADD COLUMN IF NOT EXISTS tc_id int4 NULL;
ALTER TABLE cx.bi_steps_relations ADD COLUMN IF NOT EXISTS operation_id int4 NULL;
ALTER TABLE cx.bi_steps_relations ADD COLUMN IF NOT EXISTS interface_id int4 NULL;
ALTER TABLE cx.bi_steps_relations ADD COLUMN IF NOT EXISTS user_id int4 NULL;


ALTER TABLE cx.bi_steps_relations DROP COLUMN IF EXISTS entity_type;
ALTER TABLE cx.bi_steps_relations DROP COLUMN IF EXISTS entity_id;

CREATE SEQUENCE IF NOT EXISTS cx.bi_steps_relations_id_seq
    INCREMENT BY 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1
    NO CYCLE;


ALTER TABLE cx.bi_steps_relations
    ALTER COLUMN id SET DEFAULT nextval('cx.bi_steps_relations_id_seq'::regclass);

ALTER TABLE cx.bi_steps
ALTER COLUMN latency TYPE numeric USING latency::numeric;

ALTER TABLE cx.bi_steps
ALTER COLUMN error_rate TYPE numeric USING error_rate::numeric;

ALTER TABLE cx.bi_steps
ALTER COLUMN rps TYPE numeric USING rps::numeric;