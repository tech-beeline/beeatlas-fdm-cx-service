ALTER TABLE cx.cj
    ADD COLUMN id_business_owner int4 NULL;

ALTER TABLE cx.cj
    ADD CONSTRAINT fk_cj_business_owner
        FOREIGN KEY (id_business_owner)
            REFERENCES user_auth.user_profile (id);

CREATE INDEX ix_cj_business_owner ON cx.cj USING btree (id_business_owner);

CREATE SEQUENCE cx.cj_tech_owners_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE cx.cj_tech_owners (
    id int4 NOT NULL DEFAULT nextval('cx.cj_tech_owners_id_seq'),
    id_cj int4 NOT NULL,
    id_user_profile int4 NULL,
    CONSTRAINT pk_cj_tech_owners PRIMARY KEY (id),
    CONSTRAINT uniq_tech_owner UNIQUE (id_cj, id_user_profile),
    CONSTRAINT fk_cj_tech_owners_cj
        FOREIGN KEY (id_cj)
            REFERENCES cx.cj (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_cj_tech_owners_user
        FOREIGN KEY (id_user_profile)
            REFERENCES user_auth.user_profile (id)
            ON DELETE SET NULL
);

CREATE INDEX idx_cj_tech_owners_cj ON cx.cj_tech_owners (id_cj);
CREATE INDEX idx_cj_tech_owners_user ON cx.cj_tech_owners (id_user_profile);

COMMENT ON TABLE cx.cj_tech_owners IS 'Таблица связи между CJ и техническими ответственными.';
COMMENT ON COLUMN cx.cj_tech_owners.id IS 'Уникальный идентификатор связи (генерируется автоматически).';
COMMENT ON COLUMN cx.cj_tech_owners.id_cj IS 'Идентификатор CJ, к которому привязан технический ответственный.';
COMMENT ON COLUMN cx.cj_tech_owners.id_user_profile IS 'Идентификатор пользователя (user_profile), назначенного техническим ответственным.';

