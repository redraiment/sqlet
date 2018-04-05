create table session.me (id varchar(2048) primary key);
create view session.attributes as select name, value from session.remote_attributes inner join session.me on remote_attributes.id = me.id;
create trigger session.attributes_insert_forward instead of insert on session.attributes for each row call "me.zzp.sqlet.session";
create trigger session.attributes_update_forward instead of update on session.attributes for each row call "me.zzp.sqlet.session";
create trigger session.attributes_delete_forward instead of delete on session.attributes for each row call "me.zzp.sqlet.session";

create schema request;
create table request.content (
  remote_address varchar(64) not null default '',
  server_name varchar(256) not null default '',
  server_port int not null default 0,
  method varchar(16) not null default '',
  scheme varchar(16) not null default '',
  uri varchar(4096) not null default '',
  query varchar(4096) not null default '',
  character_encoding varchar(16) not null default '',
  content_type varchar(128) not null default '',
  content_length bigint not null default 0,
  body varchar(10485760) not null default '' -- 10MB
);
create table request.headers (
  name varchar(2048) primary key,
  value varchar(2048) not null
);
create table request.cookies (
  name varchar(2048) primary key,
  value varchar(2048) not null
);
create table request.query_params (
  name varchar(2048) primary key,
  value varchar(2048) not null
);
create table request.form_params (
  name varchar(2048) primary key,
  value varchar(2048) not null
);
create view request.params as select * from request.form_params union all select * from request.query_params;


create schema response;
create table response.content (
  code int not null default 200,
  content_type varchar(128) not null default 'application/json; charset=utf-8',
  body varchar(10485760) not null default '' -- 10MB
);
create table response.headers (
  name varchar(2048) primary key,
  value varchar(2048) not null
);
create table response.cookies (
  name varchar(2048) primary key,
  value varchar(2048) not null default '',
  domain varchar(2048),
  max_age bigint,
  expires varchar(128),
  secure boolean,
  http_only boolean
);
