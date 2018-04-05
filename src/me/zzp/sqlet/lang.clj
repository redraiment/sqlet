(ns me.zzp.sqlet.lang
  "Clojure语言增强"
  (:require [clojure
             [pprint :refer [cl-format]]
             [test :refer [is]]]
            [me.zzp.sqlet
             [coll :refer [leaves walk]]
             [id :refer [id?
                         placeholder
                         interpolate]]]))

;;; control

(defn first-of
  "返回第一个非nil结果"
  {:test #(is (= :six (first-of (fn [index]
                                  (when (> index 5)
                                    :six))
                                (range 10))))}
  [f coll]
  (first (drop-while nil? (map f coll))))

;;; template

(defmacro template-for
  "代码模板，可通过 `$xx$` 占位符批量生成代码
  bindings兼容doseq和for
  *注* 因为代码在编译期展开，因此bindings中不支持引用外部变量
  必须都是常量，但支持表达式
  示例：
      (template-for [[index ordinal] (map-indexed vector '(first second third fourth fifth))]
        (defn my-$ordinal$ [v]
          (nth v $index$ nil)))
  生成 my-first、my-second 等"
  [bindings & code]
  (->> code
    leaves
    (filter id?)
    (map str)
    (mapcat (partial re-seq placeholder))
    (map second)
    (into #{})
    (map (fn [token] [token (symbol token)]))
    (into {})
    (list 'for bindings)
    eval
    (mapcat #(walk (partial interpolate %) code))
    (cons 'do)))

;;; fn

;;; fn - monad安全检查

(defn- wrap-body-when-not-nil
  "对参数+代码块的数据包装参数nil检测"
  [[args & codes :as body]]
  (if-not (empty? args)
    `(~args
      (when (not-any? nil? ~args)
        ~@codes))
    body))

(defn- wrap-define-when-not-nil
  "对代码定义包装参数nil检测"
  [f codes]
  (if (some vector? codes)
    (let [[prefix suffix] (split-with (complement vector?) codes)]
      `(~f ~@prefix ~@(wrap-body-when-not-nil suffix)))
    (let [[prefix suffixes] (split-with (complement list?) codes)]
      `(~f ~@prefix ~@(map wrap-body-when-not-nil suffixes)))))

(defmacro monad
  "当`fn`有任何一个参数为nil时返回nil，否则执行相应代码"
  [& codes]
  (wrap-define-when-not-nil 'fn codes))

(defmacro defmonad
  "当`defn`有任何一个参数为nil时返回nil，否则执行相应代码"
  [& codes]
  (wrap-define-when-not-nil 'defn codes))

(defmacro defmonad-
  "当`defn`有任何一个参数为nil时返回nil，否则执行相应代码"
  [& codes]
  (wrap-define-when-not-nil 'defn- codes))

;;; fn - 其他简写

(defmacro ^{:doc "defn + memoize"
            :arglists '([name doc-string? [params*] exprs*]
                        [name doc-string? ([params*] exprs*) +])}
  defmemoize
  [name & [doc-string & codes :as body]]
  (if (string? doc-string)
    `(def ~name ~doc-string (memoize (fn ~@codes)))
    `(def ~name (memoize (fn ~@body)))))

;;; 补齐序数词 third, fourth, ..., ninety-ninth

(template-for [[index cardinal ordinal]
               (map (fn [index]
                      [(dec index) index
                       (symbol (cl-format false "~:r" index))])
                    (drop 3 (range 100)))]
  (defn $ordinal$
    "返回序列中第$cardinal$个值
  如果序列为nil或第$cardinal$个值不存在，返回nil"
    {:test (fn []
             (is (= $index$ ($ordinal$ (range 100))))
             (is (nil? ($ordinal$ []))))}
    [v]
    (nth v $index$ nil)))
