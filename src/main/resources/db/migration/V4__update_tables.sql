
ALTER TABLE business_iteraction
    ALTER COLUMN feelings DROP NOT NULL,
ALTER COLUMN ucs_reaction DROP NOT NULL;

ALTER TABLE bi_participants
    ALTER COLUMN descr DROP NOT NULL,
ALTER COLUMN value DROP NOT NULL;

ALTER TABLE business_iteraction
    ADD COLUMN metrics varchar(300),
ADD COLUMN deleted_date timestamp without time zone,
ADD COLUMN author_id integer NOT NULL DEFAULT 0;
ALTER TABLE business_iteraction
 RENAME COLUMN dt_updated TO last_modified_date;
ALTER TABLE business_iteraction
 RENAME COLUMN dt_created TO created_date;

ALTER TABLE cj
    ADD COLUMN deleted_date timestamp without time zone,
ADD COLUMN created_date timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE cj
RENAME COLUMN last_updated TO last_modified_date;

ALTER TABLE cj_steps
    ADD COLUMN description varchar(300);