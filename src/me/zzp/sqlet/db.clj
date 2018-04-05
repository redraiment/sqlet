(ns me.zzp.sqlet.db
  "A lightweight wrapper to H2 SQL databases via JDBC."
  (:require [clojure.string :as cs]
            [me.zzp.sqlet
             [file :refer [mktempdir]]
             [logging :as log]
             [schedule :refer [at-exit]]])
  (:import java.sql.DriverManager
           org.h2.tools.Server)
  (:gen-class))

(def default-url
  "URL of global shared database. For application and session scope attributes."
  (atom nil))

(def ^:dynamic *connection*
  "Current database connection"
  nil)

(defn with-connection
  "Bind `*connection*` to the connection of specified `URL`, then invoke the
  `callback`, and close automatically at the end."
  [url callback]
  (with-open [connection (DriverManager/getConnection url)]
    (binding [*connection* connection]
      (callback))))

(defmacro sandbox
  "Create new in-memory database (sandbox) for each HTTP request."
  [& body]
  `(with-connection "jdbc:h2:mem:" (bound-fn [] ~@body)))

(defn- with-statement
  "Get or create DB connection, and invoke `callback` with prepared statement."
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
  "Executes the given SQL statement, which returns a list of hashmap for result
  records."
  [sql & parameters]
  (with-statement sql parameters
    #(with-open [rs (.getResultSet %)]
       (doall (resultset-seq rs)))))

(defn one
  "Executes the given SQL statement, which returns a hashmap for the first row."
  [sql & parameters]
  (first (apply all sql parameters)))

(defn value
  "Executes the given SQL statement, which returns a value for the first column
  of the first row."
  [sql & parameters]
  (second (first (apply one sql parameters))))

;;; update

(defn execute
  "Executes the given SQL statement, which may be an INSERT, UPDATE, or DELETE
  statement or an SQL statement that returns nothing, such as an SQL DDL statement."
  [sql & parameters]
  (with-statement sql parameters (memfn getUpdateCount)))

(defn runscript
  "Short for `runscript from <filename>`"
  [filename]
  (execute "runscript from ?" filename))

;;; launcher

(defn start
  "Start global shared H2 tcp server."
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
