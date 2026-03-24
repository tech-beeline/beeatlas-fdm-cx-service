ALTER TABLE cx.cj
    ADD COLUMN unique_ident varchar(50);

UPDATE cx.cj
SET unique_ident = 'CJ.' ||
                   lpad((id / 1000000)::text, 2, '0') || '.' ||
                   lpad(((id / 10000) % 100)::text, 2, '0') || '.' ||
                   lpad(((id / 100) % 100)::text, 2, '0') || '.' ||
                   lpad((id % 100)::text, 2, '0');

ALTER TABLE cx.cj
    ALTER COLUMN unique_ident SET NOT NULL;

COMMENT ON COLUMN cx.cj.unique_ident IS 'Уникальный идентификатор CJ (цифробуквенный).
Формируется как префикс "CJ." и значение из id, с нулями, дополненными слева до 8 символов.';

ALTER TABLE cx.cj
    ADD COLUMN IF NOT EXISTS id_product_ext int4;

ALTER TABLE cx.cj
    ALTER COLUMN id_product_ext SET NOT NULL;

ALTER TABLE cx.business_iteraction
    ADD COLUMN IF NOT EXISTS id_product_ext int4;

ALTER TABLE cx.business_iteraction
    ALTER COLUMN id_product_ext SET NOT NULL;
