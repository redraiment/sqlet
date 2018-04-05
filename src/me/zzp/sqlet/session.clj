(ns me.zzp.sqlet.session
  "会话相关"
  (:require [me.zzp.sqlet
             [db :as db]
             [logging :as log]
             [schedule :refer [schedule]]])
  (:gen-class :main false
              :implements [org.h2.api.Trigger]))

(defn -init [this connection schema trigger table before? type])

(defn -fire
  [this connection old new]
  (binding [db/*connection* connection]
    (let [id (db/value "select id from session.me limit 1")]
      (if-let [[name value] new]
        (db/execute "merge into session.remote_attributes key (id, name) values (?, ?, ?)" id name value)
        (db/execute "delete from session.remote_attributes where id = ? and name = ?" id (first old))))))

(defn -close [this])

(defn -remove [this])

(defn- link
  ([from]
   (link from from))
  ([from to]
   (format "create linked table %s('org.h2.Driver', '%s', '', '', '%s')" to @db/default-url from)))

(defn initialize
  ""
  []
  (run! db/execute
        ["create schema app"
         (link "app.attributes")
         "create schema session"
         (link "session.sessions")
         (link "session.attributes" "session.remote_attributes")])
  (db/runscript "classpath:/db/init-request.sql"))

(defn cleanup
  ""
  []
  (db/execute "delete from session.attributes where id in (select id from session.sessions where expired_at < current_timestamp)")
  (db/execute "delete from session.sessions where expired_at < current_timestamp"))

(defn start
  ""
  []
  (schedule {:fixed-delay 60000} cleanup))
