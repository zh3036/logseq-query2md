(ns extract-recursive
  "Extract blocks with references and their children recursively"
  (:require ["fs" :as fs]
            ["path" :as path]
            ["os" :as os]
            [datascript.transit :as dt]
            [datascript.core :as d]
            [clojure.pprint :as pprint]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [nbb.core :as nbb])
  (:refer-clojure :exclude [exists?]))

;; Track progress
(def fetch-count (atom 0))

;; fs utils (copied from simple-query.cljs)
(defn expand-home
  [path]
  (path/join (os/homedir) path))

(defn entries
  [dir]
  (fs/readdirSync dir))

(defn slurp
  [file]
  (.toString (fs/readFileSync file)))

(defn spit
  [file content]
  (fs/writeFileSync file content))

(defn exists?
  [file]
  (fs/existsSync file))

(defn directory?
  [file]
  (.isDirectory (fs/lstatSync file)))

;; graph utils (copied from simple-query.cljs)
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

;; New recursive block fetching functions
(defn fetch-children-recursively 
  "Recursively fetch all children of a block"
  [db parent-id level]
  (let [indent (apply str (repeat level "  "))
        _ (println (str indent "Fetching children for block ID:" parent-id))
        
        ;; Construct queries as strings and parse them at runtime
        child-ids-query-str (str "[:find ?c :where [?c :block/parent " parent-id "]]")
        child-ids-query (edn/read-string child-ids-query-str)
        _ (println (str indent "Running query: " child-ids-query-str))
        
        ;; Find all child IDs
        child-ids (try
                   (mapv first (d/q child-ids-query db))
                   (catch :default e
                     (println (str indent "Error querying children: " (.-message e)))
                     []))
        
        ;; Pull attributes for each child
        pull-pattern [:db/id :block/content :block/uuid {:block/page [:block/original-name :db/id]}]
        child-blocks (when (seq child-ids)
                      (try
                        (mapv #(d/pull db pull-pattern %) child-ids)
                        (catch :default e
                          (println (str indent "Error pulling child data: " (.-message e)))
                          [])))]
    
    (when (seq child-blocks)
      (swap! fetch-count + (count child-blocks))
      (println (str indent "Found " (count child-blocks) " children, total processed: " @fetch-count))
      
      (->> child-blocks
           (mapv (fn [child-map]
                   (let [_ (when (:block/content child-map)
                             (println (str indent "  Child block: " 
                                          (subs (:block/content child-map) 0 
                                               (min 30 (count (:block/content child-map))))
                                          (when (> (count (:block/content child-map)) 30) "..."))))
                         grand-children (fetch-children-recursively db (:db/id child-map) (inc level))]
                     (cond-> {:content (:block/content child-map)
                              :uuid (:block/uuid child-map)
                              :page (when-let [p (:block/page child-map)]
                                      (or (:block/original-name p) (:db/id p)))}
                       (seq grand-children) (assoc :children grand-children)))))
           (remove nil?)))))

(defn process-root-block 
  "Process a root block and all its children recursively"
  [db root-block block-number total-blocks]
  (println (str "\nProcessing root block " block-number "/" total-blocks ": "
                (subs (:block/content root-block) 0 
                      (min 40 (count (:block/content root-block))))
                (when (> (count (:block/content root-block)) 40) "...")
                " (ID: " (:db/id root-block) ")"))
  
  (let [children (fetch-children-recursively db (:db/id root-block) 1)]
    (cond-> {:content (:block/content root-block)
             :uuid (:block/uuid root-block)
             :page (when-let [p (:block/page root-block)]
                     (or (:block/original-name p) (:db/id p)))}
      (seq children) (assoc :children children))))

;; Main extraction function
(defn extract-and-group [db query-str output-file]
  (println "\n==== Starting extraction process ====")
  (reset! fetch-count 0)
  
  (try
    (println "Parsing query: " query-str)
    (let [query (try
                  (edn/read-string query-str)
                  (catch :default e
                    (println "Error parsing query: " (.-message e))
                    nil))
          _ (when query (println "Executing query..."))
          
          ;; Execute the query safely
          results (if query
                    (try
                      (d/q query db)
                      (catch :default e
                        (println "Error executing query: " (.-message e))
                        []))
                    [])
          
          ;; Each result is expected to be a vector with one element (the pulled entity)
          root-blocks (mapv first results)]
      
      (println (str "Found " (count root-blocks) " root blocks from query"))
      (println "Beginning recursive fetch of all children...")
      
      (let [transformed-data 
            (if (seq root-blocks)
              (->> root-blocks
                   (map-indexed (fn [idx block]
                                  (process-root-block db block (inc idx) (count root-blocks))))
                   (remove nil?)
                   (into []))
              [])]
        
        (println (str "\n==== Processing complete ===="))
        (println (str "Total blocks processed: " @fetch-count 
                     " (includes " (count root-blocks) " root blocks and their descendants)"))
        
        (if (seq transformed-data)
          (do
            (println (str "Writing results to: " output-file))
            (spit output-file (with-out-str (pprint/pprint transformed-data)))
            (println "Write successful."))
          (do
            (println "No data found. Writing empty array.")
            (spit output-file "[]")))
        
        transformed-data))
    (catch :default e
      (println "Error executing query:" (.-message e))
      [])))

;; Main run function
(defn run-extraction [graph-name query-file output-file]
  (println "Trying to run extraction on graph:" graph-name)
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
                  _ (println "Query string:" query-str)]
              (extract-and-group db query-str output-file)))
          (println "Query file not found:" query-file)))
      (println "Graph not found:" graph-name))))

;; Main function
(defn -main [args]
  (if (< (count args) 3)
    (println "Usage: extract-recursive GRAPH_NAME QUERY_FILE OUTPUT_FILE
Run DataScript query and recursively fetch all child blocks.

Arguments:
  GRAPH_NAME   Name of the Logseq graph to query
  QUERY_FILE   Path to EDN file containing the query
  OUTPUT_FILE  Path to save the extracted blocks and their children")
    (let [[graph-name query-file output-file] args]
      (run-extraction graph-name query-file output-file))))

;; Run when loaded directly
(when (= nbb.core/*file* (:file (meta #'-main)))
  (-main *command-line-args*))
