-- Front-end doesn't send clientScenario/ucsReaction/feeling/status on BI creation,
-- and SFDM-4015 requires only "name" to be mandatory. Without dropping these
-- NOT NULL constraints, removing the app-level validation would just replace the
-- clean 400 (see cfabad0 / SFDM-3976) with a raw DB constraint-violation 500.

ALTER TABLE cx.business_iteraction
    ALTER COLUMN feelings DROP NOT NULL;
ALTER TABLE cx.business_iteraction
    ALTER COLUMN status_id DROP NOT NULL;
ALTER TABLE cx.business_iteraction
    ALTER COLUMN client_scenario DROP NOT NULL;
ALTER TABLE cx.business_iteraction
    ALTER COLUMN ucs_reaction DROP NOT NULL;
