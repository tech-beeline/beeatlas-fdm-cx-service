DROP TABLE IF EXISTS bi_steps CASCADE;
DROP TABLE IF EXISTS bi_steps_type_enum CASCADE;
DROP TABLE IF EXISTS bi_steps_relations CASCADE;

ALTER TABLE cx.business_iteraction
    ADD CONSTRAINT pk_business_iteraction PRIMARY KEY (id);

CREATE TABLE bi_steps
(
    id           integer     NOT NULL, -- Идентификатор шага бизнес взаимодействия
    id_bi        integer     NOT NULL,
    id_step_type integer     NOT NULL,
    name         varchar(50) NOT NULL,
    id_bpmn      varchar(50) NOT NULL,
    latency      integer NULL,
    error_rate   integer NULL,
    rps          integer NULL,
    CONSTRAINT pk_bi_steps PRIMARY KEY (id)
);

CREATE TABLE bi_steps_type_enum
(
    id   integer     NOT NULL, -- Идентификатор типа шага бизнес взаимодействия
    name varchar(50) NOT NULL, -- Название типа шага бизнес взаимодействия
    CONSTRAINT pk_bi_steps_type_enum PRIMARY KEY (id)
);

CREATE TABLE bi_steps_relations
(
    id          integer     NOT NULL, -- Идентификатор связи шага с реализацией
    id_bi_steps  integer     NOT NULL,
    entity_type varchar(50) NOT NULL,
    entity_id   integer     NOT NULL,
    "order"       integer NULL,
    CONSTRAINT pk_bi_steps_relations PRIMARY KEY (id)
);

ALTER TABLE bi_steps
    ADD CONSTRAINT fk_bi_steps_business_iteraction FOREIGN KEY (id_bi) REFERENCES business_iteraction (id);
ALTER TABLE bi_steps
    ADD CONSTRAINT fk_bi_steps_type_enum FOREIGN KEY (id_step_type) REFERENCES bi_steps_type_enum (id);
ALTER TABLE bi_steps_relations
    ADD CONSTRAINT fk_bi_steps_relations_bi_steps FOREIGN KEY (id_bi_steps) REFERENCES bi_steps (id);

CREATE INDEX idx_bi_steps_id_bi ON bi_steps (id_bi);
CREATE INDEX idx_bi_steps_id_step_type ON bi_steps (id_step_type);
CREATE INDEX idx_bi_steps_relations_id_bi_steps ON bi_steps_relations (id_bi_steps);

ALTER TABLE cx.cj
    ALTER COLUMN b_draft DROP NOT NULL;
ALTER TABLE cx.cj
    ALTER COLUMN id_author DROP NOT NULL;
ALTER TABLE cx.cj
    ALTER COLUMN id_product_ext DROP NOT NULL;

ALTER TABLE cx.business_iteraction
    ALTER COLUMN b_communal DROP NOT NULL;
ALTER TABLE cx.business_iteraction
    ALTER COLUMN id_product_ext DROP NOT NULL;
ALTER TABLE cx.business_iteraction
    ALTER COLUMN status_id DROP NOT NULL;
ALTER TABLE cx.business_iteraction
    ALTER COLUMN client_scenario DROP NOT NULL;
ALTER TABLE cx.business_iteraction
    ALTER COLUMN b_target DROP NOT NULL;
ALTER TABLE cx.business_iteraction
    ALTER COLUMN b_draft DROP NOT NULL;

ALTER TABLE cx.cj_steps
    ADD COLUMN IF NOT EXISTS id_bpmn varchar (50) NULL;
COMMENT
ON COLUMN cx.cj_steps.id_bpmn IS ' id элемента внутри bpmn файла';

ALTER TABLE cx.business_iteraction
    ADD COLUMN IF NOT EXISTS id_bpmn varchar (50) NULL;
COMMENT
ON COLUMN cx.business_iteraction.id_bpmn IS ' id элемента внутри bpmn файла';
