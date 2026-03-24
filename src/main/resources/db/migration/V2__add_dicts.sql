INSERT INTO bi_status (ID, NAME, DESCR)
VALUES (1, 'Передан в эксплуатацию', 'Передан в эксплуатацию');

INSERT INTO bi_status (ID, NAME, DESCR)
VALUES (2, 'Не передан', 'Не передан');

INSERT INTO bi_status (ID, NAME, DESCR)
VALUES (3, 'Неизвестно', 'Неизвестно');

INSERT INTO bi_channels_enum (ID, NAME)
VALUES (1, 'Web site') ON CONFLICT DO NOTHING;

INSERT INTO bi_channels_enum (ID, NAME)
VALUES (2, 'Интернет магазин') ON CONFLICT DO NOTHING;

INSERT INTO bi_channels_enum (ID, NAME)
VALUES (3, 'Мобильное приложение') ON CONFLICT DO NOTHING;

INSERT INTO bi_channels_enum (ID, NAME)
VALUES (4, 'Личный кабинет') ON CONFLICT DO NOTHING;

INSERT INTO bi_channels_enum (ID, NAME)
VALUES (5, 'Партнерские витрины') ON CONFLICT DO NOTHING;

INSERT INTO bi_channels_enum (ID, NAME)
VALUES (6, 'Собственные офисы продаж') ON CONFLICT DO NOTHING;

INSERT INTO bi_channels_enum (ID, NAME)
VALUES (7, 'Офисы продаж (мультибренд)') ON CONFLICT DO NOTHING;

INSERT INTO bi_channels_enum (ID, NAME)
VALUES (8, 'Офисы продаж (франшиза)') ON CONFLICT DO NOTHING;

INSERT INTO bi_channels_enum (ID, NAME)
VALUES (9, 'Персональная поддержка') ON CONFLICT DO NOTHING;

INSERT INTO bi_channels_enum (ID, NAME)
VALUES (10, 'Поддержка') ON CONFLICT DO NOTHING;

INSERT INTO bi_participant_enum (ID, NAME)
VALUES (1, 'Участник со стороны клиента') ON CONFLICT DO NOTHING;

INSERT INTO bi_participant_enum (ID, NAME)
VALUES (2, 'Участник со стороны компании') ON CONFLICT DO NOTHING;

INSERT INTO bi_participant_enum (ID, NAME)
VALUES (3, 'Внешний участник') ON CONFLICT DO NOTHING;

INSERT INTO bi_feelings_enum (ID, NAME)
VALUES (0, 'Раздражен') ON CONFLICT DO NOTHING;

INSERT INTO bi_feelings_enum (ID, NAME)
VALUES (1, 'Огорчён') ON CONFLICT DO NOTHING;

INSERT INTO bi_feelings_enum (ID, NAME)
VALUES (2, 'Нейтрален') ON CONFLICT DO NOTHING;

INSERT INTO bi_feelings_enum (ID, NAME)
VALUES (3, 'Удовлетворен') ON CONFLICT DO NOTHING;

INSERT INTO bi_feelings_enum (ID, NAME)
VALUES (4, 'Доволен') ON CONFLICT DO NOTHING;

INSERT INTO link_enum
(id, type)
VALUES (1, 'Ссылка на флоу') ON CONFLICT DO NOTHING;

INSERT INTO link_enum
(id, type)
VALUES (2, 'Документ') ON CONFLICT DO NOTHING;

INSERT INTO link_enum
(id, type)
VALUES (3, 'Макет') ON CONFLICT DO NOTHING;