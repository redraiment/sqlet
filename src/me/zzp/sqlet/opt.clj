(ns me.zzp.sqlet.opt
  "全局选项，用于功能插件的初始化。
  !!!非应用配置项!!!

  加载过程
  1. `sqlet.` 开头的系统属性（System Property），但不需要指定`sqlet`
  2. resource://sqlet.edn，其中不包含`sqlet`
  为方便上游使用，会对System Property的值做类型转换，而不总是返回字符串
  * 大小写无关的 \"true\", \"yes\"：返回 Boolean 值 `true`
  * 大小写无关的 \"false\", \"no\"：返回 Boolean 值 `false`
  * `\\d+`：返回整数
  * `\\d+\\.\\d*`：返回浮点数
  * 其他：返回字符串

  例如：
  1、`sqlet.edn`的内容如下
  ```edn
  {:a {:b 1
       :c 2}}
  ```
  2、启动参数包含`-Dsqlet.a.b=3`
  则`(sqlet/of [:a :b])`返回整数`3`"
  (:require [clojure.string :as cs]
            [me.zzp.sqlet
             [coercion :refer [as-long as-double]]
             [file :refer [edn-of]]
             [id :refer [id]]]))

(def ^:dynamic *options*
  "配置文件选项"
  (delay (edn-of "resource://sqlet.edn")))

(defn- to-property-name
  "将给定的关键字列表转换成点号分隔的属性名称"
  [ks]
  (if (coll? ks)
    (cs/join "." (map id ks))
    (id ks)))

(defn- to-key-list
  "将给定的点号分隔的属性名称转换成关键字列表"
  [property-name]
  (mapv keyword (cs/split property-name #"\.")))

(defn system-property?
  "判断给定的key是否存在于系统属性中"
  [key]
  (.. (System/getProperties)
      stringPropertyNames
      (contains key)))

(defn- parse
  "字符串值解析
  * 大小写无关的 \"true\", \"yes\"：返回 Boolean 值 `true`
  * 大小写无关的 \"false\", \"no\"：返回 Boolean 值 `false`
  * `\\d+`：返回整数
  * `\\d+\\.\\d*`：返回浮点数
  * 其他：返回字符串"
  [s]
  (if (string? s)
    (cond
      (re-matches #"\d+" s) (as-long s)
      (re-matches #"\d+\.\d*" s) (as-double s)
      (#{"true" "yes"} (.toLowerCase s)) true
      (#{"false" "no"} (.toLowerCase s)) false
      :else s)
    s))

(defmacro ^:private defn-default
  "定义包含默认值的函数"
  [name comment [argument] & body]
  `(defn ~name ~comment
     ([~argument] (~name ~argument nil))
     ([~argument ~'default-value] ~@body)))

(defn-default property
  "返回系统属性
  keys按照点号拼接作为key"
  [ks]
  (let [key (to-property-name ks)]
    (if (system-property? key)
      (System/getProperty key)
      default-value)))

(defn-default properties
  "根据前缀prefix返回hash-map形式的多个系统属性"
  [prefixes]
  (let [prefix (str (to-property-name prefixes) ".")
        length (count prefix)
        values (->> (System/getProperties)
                 (map (fn [[key value]]
                        (when (cs/starts-with? key prefix)
                          [(subs key length) value])))
                 (into {}))]
    (if (seq values)
      values
      default-value)))

(defmacro ^:private defn-option
  "定义包含默认值的函数"
  [name comment & body]
  `(defn-default ~name ~comment [ks#]
     (let [~'key-list (if (coll? ks#) ks# [ks#])
           ~'property-name (to-property-name (into [:sqlet] ~'key-list))]
       ~@body)))

(defn-option of
  "返回当前上下文的选项值
  1. 先从 System.Properties 中获取 sqlet. 开头的值
  2. 再从 sqlet.edn 中获取
  返回单个值"
  (let [result (parse (property property-name (get-in @*options* key-list {})))]
    (if (coll? result)
      default-value
      result)))

(defn-option ofs
  "返回当前上下文的多个选项值
  1. 先从 System.Properties 中获取 sqlet. 开头的值
  2. 再从 sqlet.edn 中获取
  返回hash-map形式的多个值"
  (let [o (get-in @*options* key-list {})
        o (reduce (fn [m [key value]]
                    (assoc-in m (to-key-list key) (parse value)))
                  (if (map? o) o {})
                  (properties property-name))]
    (if (seq o)
      o
      default-value)))
