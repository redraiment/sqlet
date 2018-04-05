# sqlet

SQLet: SQL Server Applet.

This is a simple http server for static content, but also run purely SQL scripts
to handle request. It is nice to experiment some ideas the quick way by starting
a http server in a few seconds, without editing some config file first.

## Installation

Download from https://github.com/redraiment/sqlet and build by following steps.

```sh
git clone https://github.com/redraiment/sqlet.git
cd sqlet
lein uberjar
```

You need to install leiningen first.

## Usage

```sh
java [options] -jar sqlet-1.0.0-standalone.jar
```

Run the standalone jar, SQLet will start web server to serve the content of
current directory. The default port is 1989. For each request, it will execute
every *.sql file, and just return the content of the other files.

For each SQL request, SQLet will launch an anonymous in-memory database, and
link to a shared database for application & session scope attributes. The HTTP
request will be parsed into below tables:

* `app` schema
    * `attributes` table: global application scope configurations.
        * `name` column
        * `value` column
* `session` schema
    * `attributes` view: session scope attributes.
        * `name` column
        * `value` column
* `request` schema
    * `context` table
        * `remote_address` column: remote ip.
        * `server_name` column: server url host name.
        * `server_port` column: server port.
        * `method` column: request method.
        * `scheme` column: http, https.
        * `uri` column: path of url.
        * `query` column: query of get method.
        * `character_encoding` column
        * `content_type` column
        * `content_length` column
        * `body` column: http body string.
    * `headers` table
        * `name` column
        * `value` column
    * `cookies` table
        * `name` column
        * `value` column
    * `query_params` table: parameters of get method.
        * `name` column
        * `value` column
    * `form_params` table: parameters of post method.
        * `name` column
        * `value` column
    * `params` view: union all `query_params` and `form_params`.
        * `name` column
        * `value` column
* `response` schema
    * `context` table
        * `code` column: HTTP response code. 200 default.
        * `content_type` column: `application/json; charset=utf-8` default.
        * `body` column: response body string.
    * `headers` table
        * `name` column
        * `value` column
    * `cookies` table
        * `name` column: cookie name
        * `value` column: the new value of the cookie
        * `path` column: the subpath the cookie is valid for
        * `domain` column: the domain the cookie is valid for
        * `max_age` column: the maximum age in seconds of the cookie
        * `expires` column: a date string at which the cookie will expire
        * `secure` column: set to true if the cookie requires HTTPS, prevent HTTP access
        * `http_only` column: set to true if the cookie is valid for HTTP and HTTPS only

## Options

* `-Dsqlet.port=<port>`: Listen on port `<port>` for incoming connections.

## Examples

Save below content as `index.sql`.

```sql
insert into response.content (content_type, body) values ('text/html; charset=utf-8', '<h1>Hello world</h1>');
```

Then start SQLet server, and visit http://localhost:1989/index.sql .

## License

Copyright © 2018 FIXME

Distributed under the FreeBSD Copyright.
