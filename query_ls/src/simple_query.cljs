(ns simple-query
  "Simple tool to run DataScript queries against Logseq graphs"
  (:require ["fs" :as fs]
            ["path" :as path]
            ["os" :as os]
            [datascript.transit :as dt]
            [datascript.core :as d]
            [clojure.pprint :as pprint]
            [clojure.edn :as edn]
            [nbb.core :as nbb])
  (:refer-clojure :exclude [exists?]))

;; fs utils
(defn expand-home
  [path]
  (path/join (os/homedir) path))

(defn entries
  [dir]
  (fs/readdirSync dir))

(defn slurp
  [file]
  (.toString (fs/readFileSync file)))

(defn exists?
  [file]
  (fs/existsSync file))

(defn directory?
  [file]
  (.isDirectory (fs/lstatSync file)))

;; graph utils
(defn get-graph-paths
  []
  (let [dir (expand-home ".logseq/graphs")]
    (->> (entries dir)
         (filter #(re-find #"\.transit$" %))
         (map #(str dir "/" %)))))

(defn full-path->graph
  [path]
  (second (re-find #"\+\+([^\+]+)\.transit$" path)))

(defn get-graph-path
  [graph]
  (some #(when (= graph (full-path->graph %)) %)
        (get-graph-paths)))

(defn- get-graph-db
  [graph]
  (when-let [file (or (get-graph-path graph)
                      ;; graph is a path
                      graph)]
    (when (exists? file)
      (-> file slurp dt/read-transit-str))))

;; Run query
(defn run-query [graph-name query-file]
  (println "Trying to run query on graph:" graph-name)
  (println "Looking for graph path...")
  (let [graph-path (get-graph-path graph-name)]
    (println "Graph path:" (or graph-path "Not found"))
    
    (if-let [db (get-graph-db graph-name)]
      (do
        (println "Successfully loaded graph database")
        (if (exists? query-file)
          (do
            (println "Found query file:" query-file)
            (let [query-str (slurp query-file)
                  _ (println "Query string:" query-str)
                  query (edn/read-string query-str)
                  _ (println "Parsed query:" (pr-str query))]
              (try
                (println "Executing query...")
                (let [results (d/q query db)]
                  (println "Query results:")
                  (pprint/pprint results))
                (catch :default e
                  (println "Error executing query:" (.-message e))))))
          (println "Query file not found:" query-file)))
      (println "Graph not found:" graph-name))))

;; Main function
(defn -main [args]
  (if-not (= 2 (count args))
    (println "Usage: lq-simple GRAPH_NAME QUERY_FILE
Run DataScript query from file against Logseq graph.

Arguments:
  GRAPH_NAME   Name of the Logseq graph to query
  QUERY_FILE   Path to EDN file containing the query")
    (let [[graph-name query-file] args]
      (run-query graph-name query-file))))

;; Run when loaded directly
(when (= nbb.core/*file* (:file (meta #'-main)))
  (-main *command-line-args*))
