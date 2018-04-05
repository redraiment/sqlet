(ns me.zzp.sqlet.coll
  "collection helper functions"
  (:require [clojure.test :refer [are]]))

(defn walk
  "recursive map, and keep the original data structure."
  {:test #(are [expected o] (= expected (walk inc o))
            {1 1 2 {3 #{2 [3]}}} {0 0 1 {2 #{1 [2]}}})}
  [f o]
  (cond
    (or (seq? o) (list? o)) (map (partial walk f) o)
    (vector? o) (mapv (partial walk f) o)
    (map? o) (->> o
               (map (fn [[k v]]
                      [(walk f k)
                       (walk f v)]))
               (into {}))
    (set? o) (into #{} (map (partial walk f) o))
    :else (f o)))

(defn leaves
  "returns the value of leaf nodes"
  {:test #(are [expected o] (= expected (leaves o))
            '(:one 1 :two :three 2 3)
            {:one 1 :two {:three [2 #{3}]}})}
  [o]
  (mapcat (fn [e]
            (if (coll? e)
              (leaves e)
              [e]))
          o))
