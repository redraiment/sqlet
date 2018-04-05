-- 应用配置

create schema app;

create table app.attributes (
  name varchar(2048) primary key,
  value varchar(2048)
);

-- 会话管理

create schema session;

create table session.sessions (
  id varchar(2048) primary key,
  expired_at timestamp not null default dateadd('minute', 30, current_timestamp)
);

create table session.attributes (
  id varchar(2048) not null,
  name varchar(2048) not null,
  value varchar(2048),
  unique (id, name)
);
