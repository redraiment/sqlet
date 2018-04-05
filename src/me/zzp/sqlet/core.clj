(ns me.zzp.sqlet.core
  "SQLet: SQL Server Applet"
  (:require [me.zzp.sqlet
             [db :as db]
             [session :as session]
             [web :as web]])
  (:gen-class))

(defn -main
  [& args]
  (db/start)
  (web/start)
  (session/start))
