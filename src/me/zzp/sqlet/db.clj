(ns me.zzp.sqlet.db
  (:require [clojure.string :as cs]
            [me.zzp.sqlet
             [file :refer [mktempdir]]
             [logging :as log]
             [schedule :refer [at-exit]]])
  (:import java.sql.DriverManager
           org.h2.tools.Server)
  (:gen-class))

(def default-url
  "默认的公共数据库URL"
  (atom nil))

(def ^:dynamic *connection*
  "当前数据库对象"
  nil)

(defn with-connection
  "连接信息上下文"
  [url callback]
  (with-open [connection (DriverManager/getConnection url)]
    (binding [*connection* connection]
      (callback))))

(defmacro sandbox
  "沙盒"
  [& body]
  `(with-connection "jdbc:h2:mem:" (bound-fn [] ~@body)))

(defn- with-statement
  "语句上下文"
  [sql parameters callback]
  (if *connection*
    (with-open [statement (.prepareStatement *connection* sql)]
      (doseq [[index o] (map-indexed vector parameters)]
        (.setObject statement (inc index) o))
      (.execute statement)
      (callback statement))
    (with-connection @default-url
      (bound-fn []
        (with-statement sql parameters callback)))))

;;; query

(defn all
  "返回查询结果"
  [sql & parameters]
  (with-statement sql parameters
    #(with-open [rs (.getResultSet %)]
       (doall (resultset-seq rs)))))

(defn one
  "返回第一行结果"
  [sql & parameters]
  (first (apply all sql parameters)))

(defn value
  "返回第一行第一列结果"
  [sql & parameters]
  (second (first (apply one sql parameters))))

;;; update

(defn execute
  "执行更新语句"
  [sql & parameters]
  (with-statement sql parameters (memfn getUpdateCount)))

(defn runscript
  "runscript from <filename>"
  [filename]
  (execute "runscript from ?" filename))

;;; launcher

(defn start
  "启动公共H2数据库服务器"
  []
  (let [dir (mktempdir "sqlet")
        options (into-array String ["-tcp" "-baseDir" dir])
        tcp (Server/createTcpServer options)]
    (.start tcp)
    (at-exit (fn []
               (.stop tcp)
               (log/info "h2 server stop")))
    (reset! default-url (format "jdbc:h2:tcp://localhost:%d/sqlet" (.getPort tcp)))
    (runscript "classpath:/db/init-app.sql")
    (log/info "h2 server started @ {}" @default-url)))
