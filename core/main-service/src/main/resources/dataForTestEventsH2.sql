insert into categories (name) values ('Концерты');
insert into categories (name) values ('Выставки');
insert into categories (name) values ('Спорт');

insert into users (name, email) values ('Иван Петров', 'ivan@mail.ru');
insert into users (name, email) values ('Мария Петрова', 'maria@mail.ru');

insert into events (
    annotation,
    category_id,
    description,
    event_date,
    initiator_id,
    paid,
    participant_limit,
    request_moderation,
    title,
    state,
    created_on,
    published_on,
    lat,
    lon
) values (
    'Тестовый футбольный матч - аннотация',
    3,
    'Тестовый футбольный матч - описание',
    timestamp '2026-08-01 15:00:00',
    1,
    false,
    20,
    true,
    'Тестовый футбольный матч - заголовок',
    'PUBLISHED',
    timestamp '2026-01-01 10:00:00',
    timestamp '2026-01-01 10:00:00',
    55.7558,
    37.6173
);

insert into events (
    annotation,
    category_id,
    description,
    event_date,
    initiator_id,
    paid,
    participant_limit,
    request_moderation,
    title,
    state,
    created_on,
    lat,
    lon
) values (
    'Тестовая выставка - заголовок',
    2,
    'Тестовая выставка - описание',
    timestamp '2026-08-01 15:00:00',
    2,
    false,
    20,
    true,
    'Тестовая выставка - заголовок',
    'PENDING',
    timestamp '2026-01-01 10:00:00',
    55.7558,
    37.6173
);

insert into events (
    annotation,
    category_id,
    description,
    event_date,
    initiator_id,
    paid,
    participant_limit,
    request_moderation,
    title,
    state,
    created_on,
    lat,
    lon
) values (
    'Тестовый концерт - заголовок',
    1,
    'Тестовый концерт - описание',
    timestamp '2026-08-01 15:00:00',
    2,
    false,
    20,
    true,
    'Тестовый концерт - заголовок',
    'PENDING',
    timestamp '2026-01-01 10:00:00',
    55.7558,
    37.6173
);

insert into comments (
  text, published_date, edited_on, author_id, event_id
) values (
  'тестовый комментарий от юзера-1 к событию-1', current_timestamp, null, 1, 1
);

insert into comments (
  text, published_date, edited_on, author_id, event_id
) values (
  'тестовый комментарий от юзера-2 к событию-1', current_timestamp, null, 2, 1
);

insert into comments (
  text, published_date, edited_on, author_id, event_id
) values (
  'тестовый комментарий от юзера-1 к событию-2', current_timestamp, null, 1, 2
);

insert into comments (
  text, published_date, edited_on, author_id, event_id
) values (
  'тестовый комментарий от юзера-2 к событию-2', current_timestamp, null, 2, 2
);

insert into comments (
  text, published_date, edited_on, author_id, event_id
) values (
  'тестовый комментарий от юзера-1 к событию-2', current_timestamp, null, 1, 2
);

insert into comments (
  text, published_date, edited_on, author_id, event_id
) values (
  'тестовый комментарий от юзера-2 к событию-3', current_timestamp, null, 2, 2
);