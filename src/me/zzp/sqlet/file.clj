(ns me.zzp.sqlet.file
  "文件系统相关函数"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as cs])
  (:import [java.io File PushbackReader]
           [java.nio.file Files Path Paths
            FileVisitOption LinkOption CopyOption StandardCopyOption]
           java.nio.file.attribute.FileAttribute))

;;; default options

(def ^:private link-options
  "文件链接选项：为空默认处理文件链接"
  (make-array LinkOption 0))

(def ^:private file-attributes
  "文件属性"
  (make-array java.nio.file.attribute.FileAttribute 0))

(def ^:private file-visit-options
  "文件访问选项：为空默认不处理链接"
  (make-array FileVisitOption 0))

(def ^:private copy-options
  "文件复制选项"
  (into-array CopyOption
              [StandardCopyOption/COPY_ATTRIBUTES
               StandardCopyOption/REPLACE_EXISTING]))

(def ^:private move-options
  "文件移动选项"
  (into-array CopyOption
              [StandardCopyOption/ATOMIC_MOVE
               StandardCopyOption/REPLACE_EXISTING]))

;;; 文件操作

(defonce ^{:doc "名字分隔符"} separator
  File/separator)

(defonce ^{:doc "路径分隔符"} path-separator
  File/pathSeparator)

(defn path
  "创建java.nio.file.Path"
  [first & more]
  (if (instance? Path first)
    first
    (Paths/get first (into-array String more))))

(defn file
  "创建java.io.File"
  [first & more]
  (.toFile (apply path first more)))

(defn to-file
  "转成java.io.File"
  [o]
  (cond
    (instance? File o) o
    (string? o) (File. o)
    (instance? Path o) (.toFile o)))

(defn absolute-path
  "转成绝对路径"
  [path]
  (.getCanonicalPath (to-file path)))

(defn relative-path
  "转成相对路径"
  [source target]
  (str (.relativize (path source) (path target))))

(defmacro ^:private defile
  "定义文件相关操作"
  [name comment & body]
  `(defn ~name ~comment [~'first & ~'more]
     (let [~'p (apply path ~'first ~'more)]
       ~@body)))

(defile exists?
  "文件存在"
  (Files/exists p link-options))

(defile mkdir
  "创建目录"
  (Files/createDirectories p file-attributes))

(defn mktempdir
  "创建临时目录"
  [prefix]
  (let [attributes (make-array FileAttribute 0)
        path (Files/createTempDirectory prefix attributes)
        file (to-file path)]
    (.deleteOnExit file)
    (absolute-path file)))

;;; 文件内容

(defn reader
  "增强clojure.java.io/reader
  可读取 resource:// 开头的资源文件"
  [source]
  (if (and (string? source) (cs/starts-with? source "resource://"))
    (if-let [resource (io/resource (subs source 11))]
      (io/reader resource))
    (io/reader source)))

(defn edn-of
  "从reader中读取edn数据"
  [source]
  (when-let [input (reader source)]
    (edn/read (PushbackReader. input))))
