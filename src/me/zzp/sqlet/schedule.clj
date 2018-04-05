(ns me.zzp.sqlet.schedule
  "线程调度相关函数"
  (:import [java.util.concurrent Executors TimeUnit]))

(defonce ^{:private true :doc "系统退出时的回调函数"}
  at-exit-handlers
  (atom []))

(. (Runtime/getRuntime) addShutdownHook
   (Thread. (fn []
              (doseq [f @at-exit-handlers] (f))
              (shutdown-agents))))

(defn at-exit
  "系统退出时执行的任务"
  [f]
  (swap! at-exit-handlers conj f))

;;; 时间调度

(defonce ^{:private true :doc "任务调度器"}
  scheduler
  (Executors/newScheduledThreadPool
   1
   (proxy [java.util.concurrent.ThreadFactory] []
     (^Thread newThread [^Runnable r]
      (doto (Thread. r "sqlet-schedule")
        (.setDaemon true))))))

(defn schedule
  "周期性任务调度"
  [{:keys [initial-delay fixed-rate fixed-delay]
    :or {initial-delay 0}}
   callback]
  (cond
    (integer? fixed-rate) (.scheduleAtFixedRate scheduler callback initial-delay fixed-rate TimeUnit/MILLISECONDS)
    (integer? fixed-delay) (.scheduleWithFixedDelay scheduler callback initial-delay fixed-delay TimeUnit/MILLISECONDS)
    :else (throw (IllegalArgumentException. "fixed-rate or fixed-delay should not be nil"))))

(at-exit
 (fn []
   (.shutdown scheduler)))
