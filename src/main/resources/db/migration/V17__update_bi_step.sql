alter table cx.bi_steps
    add column unique_ident varchar(50);

update
    cx.bi_steps
set
    unique_ident =
        'Step.' ||
        lpad((id / 1000000)::text,
             2,
             '0') || '.' ||
        lpad(((id / 10000) % 100)::text,
    2,
    '0') || '.' ||
        lpad(((id / 100) % 100)::text,
    2,
    '0') || '.' ||
        lpad((id % 100)::text,
             2,
             '0');

alter table cx.bi_steps
    alter column unique_ident set
        not null;

comment on
column cx.bi_steps.unique_ident
is 'Уникальный идентификатор шага BI (цифробуквенный).
Формируется как префикс "Step.", с нулями дополненными слева до 8 символов и id';