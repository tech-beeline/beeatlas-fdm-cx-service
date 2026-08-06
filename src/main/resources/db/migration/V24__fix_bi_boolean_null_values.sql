-- b_communal/b_target/b_draft were made nullable in V5, but the BI entity maps them
-- as primitive boolean fields, so Hibernate throws PropertyAccessException
-- ("Null value was assigned to a property of primitive type setter") whenever a row
-- with a NULL value in one of these columns is read (e.g. via /v1/library/business-interactions/find).
-- Backfill existing NULLs and restore the NOT NULL/DEFAULT contract expected by the entity.

UPDATE cx.business_iteraction SET b_communal = false WHERE b_communal IS NULL;
UPDATE cx.business_iteraction SET b_target = false WHERE b_target IS NULL;
UPDATE cx.business_iteraction SET b_draft = false WHERE b_draft IS NULL;

ALTER TABLE cx.business_iteraction
    ALTER COLUMN b_communal SET DEFAULT false;
ALTER TABLE cx.business_iteraction
    ALTER COLUMN b_communal SET NOT NULL;

ALTER TABLE cx.business_iteraction
    ALTER COLUMN b_target SET DEFAULT false;
ALTER TABLE cx.business_iteraction
    ALTER COLUMN b_target SET NOT NULL;

ALTER TABLE cx.business_iteraction
    ALTER COLUMN b_draft SET DEFAULT false;
ALTER TABLE cx.business_iteraction
    ALTER COLUMN b_draft SET NOT NULL;
