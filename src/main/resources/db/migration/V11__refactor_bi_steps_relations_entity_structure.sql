
ALTER TABLE cx.bi_steps_relations ADD description text NULL;
ALTER TABLE cx.bi_steps_relations ADD product_id int4 NULL;
ALTER TABLE cx.bi_steps_relations ADD tc_id int4 NULL;
ALTER TABLE cx.bi_steps_relations ADD operation_id int4 NULL;
ALTER TABLE cx.bi_steps_relations ADD interface_id int4 NULL;
ALTER TABLE cx.bi_steps_relations ADD user_id int4 NULL;


ALTER TABLE cx.bi_steps_relations DROP COLUMN entity_type;
ALTER TABLE cx.bi_steps_relations DROP COLUMN entity_id;

CREATE SEQUENCE cx.bi_steps_relations_id_seq
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