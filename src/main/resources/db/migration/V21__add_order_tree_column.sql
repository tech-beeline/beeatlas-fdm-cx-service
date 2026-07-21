ALTER TABLE cx.cj_steps
    ADD COLUMN IF NOT EXISTS order_tree varchar(50) NULL;

ALTER TABLE cx.bi_in_cj_step
    ADD COLUMN IF NOT EXISTS order_tree varchar(50) NULL;

ALTER TABLE cx.bi_steps
    ADD COLUMN IF NOT EXISTS order_tree varchar(50) NULL;

COMMENT ON COLUMN cx.cj_steps.order_tree
    IS 'Порядок этапа CJ из BPMN (1, 2, 3.1, 4.1.2)';

COMMENT ON COLUMN cx.bi_in_cj_step.order_tree
    IS 'Порядок BI в этапе CJ из BPMN (1, 2, 3.1, 4.1.2)';

COMMENT ON COLUMN cx.bi_steps.order_tree
    IS 'Порядок шага BI из BPMN (1, 2, 3.1, 4.1.2)';
