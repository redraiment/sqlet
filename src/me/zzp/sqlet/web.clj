(ns me.zzp.sqlet.web
  "Web服务"
  (:require [clojure.string :as cs]
            [org.httpkit.server :as http]
            [ring.util
             [response :refer [file-response
                               not-found]]]
            [ring.middleware
             [content-type :refer [wrap-content-type]]
             [cookies :refer [wrap-cookies]]
             [head :refer [wrap-head]]
             [params :refer [wrap-params]]]
            [me.zzp.sqlet
             [file :as file]
             [logging :as log]
             [opt :as opt]
             [schedule :refer [at-exit]]]
            [me.zzp.sqlet
             [db :as db]
             [session :as session]])
  (:import java.util.UUID))

;;; 常量

(defn- uuid
  []
  (str (java.util.UUID/randomUUID)))

(defn- build-request
  [{:keys [remote-addr server-name server-port
           request-method scheme uri query-string
           character-encoding content-type content-length body
           headers query-params form-params]
    {{token :value} "SQLET_TOKEN" :as cookies} :cookies}]
  (let [new? (cs/blank? token)
        token (if new? (uuid) token)]
    (db/execute "merge into session.sessions key (id) values (?, dateadd('minute', 30, current_timestamp))" token)
    (db/execute "insert into session.me values (?)" token)
    (when new?
      (db/execute "insert into response.cookies (name, value, max_age) values ('SQLET_TOKEN', ?, 31536000)" token)))

  (db/execute "insert into request.content values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
              (or remote-addr "") (or server-name "") (or server-port 0)
              (name (or request-method "")) (name (or scheme "")) (or uri "/") (or query-string "")
              (or character-encoding "") (or content-type "") (or content-length 0)
              (if body (slurp body) ""))

  (doseq [[table-name pairs]
          [["headers" headers]
           ["cookies" (map (fn [[name {value :value}]] [name value]) cookies)]
           ["query_params" query-params]
           ["form_params" form-params]]
          :let [sql (format "insert into request.%s values (?, ?)" table-name)]]
    (doseq [[name value] pairs]
      (db/execute sql name value))))

(defn- build-response-headers
  []
  (->> "select * from response.headers"
    db/all
    (map (fn [{:keys [name value]}]
           [name value]))
    (into {})))

(defn- build-response-cookies
  []
  (->> "select * from response.cookies"
    db/all
    (map (fn [{key :name :as record}]
           [key (dissoc (->> record
                          (remove (comp nil? second))
                          (map (fn [[key value]]
                                 [(-> key
                                    name
                                    (cs/replace #"_" "-")
                                    keyword)
                                  value]))
                          (into {}))
                        :name)]))
    (into {})))

(defn- build-response
  []
  (let [{:keys [code content_type body]}
        (db/one "select * from response.content limit 1")]
    {:status (or code 200)
     :headers (assoc (build-response-headers)
                     "Server" "SQLet"
                     "Content-Type" content_type)
     :cookies (build-response-cookies)
     :body body}))

(defn- handler [{:keys [uri request-method] :as request}]
  (let [file-path (-> uri
                    (cs/replace #"/+" "/")
                    (cs/replace #"^/" ""))]
    (if (file/exists? file-path)
      (if (cs/ends-with? file-path ".sql")
        (db/sandbox
         (session/initialize)
         (build-request request)
         (db/runscript file-path)
         (build-response))
        (if (= :get request-method)
          (file-response file-path {:root "."
                                    :index-files? false
                                    :allow-symlinks true})
          {:status 405 :headers {} :body ""})) ; method not allowed
      (not-found ""))))

(defn start
  "启动web服务器"
  []
  (let [port (opt/of [:port] 1989)]
    (when-let [server (http/run-server (-> handler
                                         wrap-params
                                         wrap-cookies
                                         wrap-head
                                         wrap-content-type)
                                       {:port port})]
      (at-exit (fn []
                 (server :timeout 100)
                 (log/info "web server stop")))
      (log/info "web server started @ {}" port))))
