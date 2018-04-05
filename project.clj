(defproject me.zzp/sqlet "1.0.0"
  :description "Build Web Service with Pure SQL"
  :url "https://github.com/redraiment/sqlet"
  :license {:name "The FreeBSD Copyright"
            :url "https://www.freebsd.org/copyright/freebsd-license.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 ;; logging
                 [org.slf4j/slf4j-api "1.7.25"]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 ;; database
                 [com.h2database/h2 "1.4.197"]
                 ;; http server & client
                 [http-kit "2.1.19"]
                 [ring/ring-core "1.6.3"]
                 ;; json
                 [clj-json "0.5.3"]]
  :main ^:skip-aot me.zzp.sqlet.core
  :test-paths ["src"]
  :target-path "target/%s"
  :aot :all
  :omit-source true)
