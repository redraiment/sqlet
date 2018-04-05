(ns me.zzp.sqlet.coercion
  "基础类型转换相关函数"
  (:require [clojure.test :refer [is are]]
            [me.zzp.sqlet.lang :refer [template-for]]))

(defn bool->long [b]
  (if b 1 0))

;;; string <-> number

(defn str->bool
  "解析字符串为boolean类型，否则返回默认值"
  {:test (fn []
           (is (str->bool "true"))
           (is (str->bool "TRUE"))
           (is (str->bool false true))
           (is (not (str->bool "false")))
           (is (not (str->bool "FALSE")))
           (is (not (str->bool "Hello"))))}
  ([s] (str->bool s false))
  ([s v] (try (Boolean/parseBoolean s)
              (catch Throwable _ v))))

(defn to-bool
  "如果是Boolean或String，则转换成boolean类型，否则返回默认值"
  {:test (fn []
           (is (to-bool true))
           (is (not (to-bool false))))}
  ([o] (to-bool o false))
  ([o v] (if (instance? Boolean o)
           o
           (str->bool o v))))

(defn as-bool
  "如果是Boolean或String，则转换成boolean类型，否则返回参数自身"
  {:test (fn []
           (is (= 1 (as-bool 1))))}
  [o]
  (to-bool o o))

(template-for [type '[byte short int long float double]]

  (defn str->$type$
    "解析字符串为$type$类型，否则返回默认值"
    {:test (fn []
             (are [expected s] (== expected (str->$type$ s))
               1 "1"
               1 "1.0"
               0 "1a")
             (is (== 1 (str->$type$ "1a" 1))))}
    ([s] (str->$type$ s ($type$ 0)))
    ([s v] (try (.$type$Value (bigdec s))
                (catch Throwable _ v))))

  (defn to-$type$
    "如果是Number或String，则转换成$type$类型，否则返回默认值"
    {:test #(are [expected s] (== expected (to-$type$ s))
              1 1
              1 1.0)}
    ([o] (to-$type$ o ($type$ 0)))
    ([o v] (if (number? o)
             (.$type$Value o)
             (str->$type$ o v))))

  (defn as-$type$
    "如果是Number或String，则转换成$type$类型，否则返回参数自身"
    {:test #(is (= "1a" (as-$type$ "1a")))}
    [o]
    (to-$type$ o o)))
