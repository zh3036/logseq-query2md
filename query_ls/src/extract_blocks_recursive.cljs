(ns extract-blocks-recursive
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.pprint :as pp]
            [datascript.core :as d]
            [logseq.graph-parser.cli :as gp-cli]
            [logseq.db.schema :as db-schema]
            [nbb.core :as nbb]
            [fs]
            [path]))

(def fetch-count (atom 0))

(defn find-graph-path [graph-name]
  (println "Looking for graph path...")
  (let [home-dir (nbb/exec "echo $HOME" #js{:encoding "utf8"})
        logseq-graphs-dir (str/trim (path/join home-dir ".logseq" "graphs"))
        encoded-graph-name (-> graph-name
                               (str/replace #" " "++")
                               (str/replace #"/" "++")
                               (str/replace #"\\" "++"))
        pattern (str "logseq_local_" encoded-graph-name)
        graph-files (fs/readdirSync logseq-graphs-dir)
        matching-file (first (filter #(str/includes? % pattern) graph-files))]
    (if matching-file
      (let [graph-path (path/join logseq-graphs-dir matching-file)]
        (println "Graph path:" graph-path)
        graph-path)
      (do
        (println "Error: Could not find graph with name" graph-name)
        nil))))

(defn load-graph-db [graph-path]
  (println "Loading graph database...")
  (try
    (let [conn (gp-cli/load-graph graph-path)
          db @conn]
      (println "Successfully loaded graph database")
      db)
    (catch js/Error e
      (println "Error loading graph:" (.-message e))
      nil)))

(defn run-query [db query-str]
  (println "Running query:" (str/trim (subs query-str 0 (min 60 (count query-str)))) 
           (when (> (count query-str) 60) "..."))
  (try
    (let [query (edn/read-string query-str)
          results (d/q query db)]
      results)
    (catch js/Error e
      (println "Error executing query:" (.-message e))
      [])))

(defn fetch-children-recursively [db parent-id level]
  (let [indent (apply str (repeat level "  "))
        _ (println (str indent "Fetching children for block ID:" parent-id))
        
        ;; Query to find all children of the block with parent-id
        query (str "[:find (pull ?c [:db/id :block/content :block/uuid {:block/page [:block/original-name :db/id]}]) :where [?c :block/parent " parent-id "]]")
        
        results (run-query db query)
        child-blocks (mapv first results)]
    
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

(defn process-root-block [db root-block block-number total-blocks]
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

(defn process-blocks [db query-file output-file]
  (println "\n==== Starting extraction process ====")
  (reset! fetch-count 0)
  
  (println "Reading query file:" query-file)
  (let [query-str (str/trim (fs/readFileSync query-file "utf8"))
        _ (println "Query string:" query-str)
        
        root-blocks-raw (run-query db query-str)
        root-blocks (mapv first root-blocks-raw)]
    
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
          (fs/writeFileSync output-file (with-out-str (pp/pprint transformed-data)))
          (println "Write successful."))
        (do
          (println "No data found. Writing empty array.")
          (fs/writeFileSync output-file "[]")))
      
      transformed-data)))

(defn -main [& args]
  (if (< (count args) 3)
    (do
      (println "Usage: npx @logseq/nbb-logseq extract_blocks_recursive.cljs <graph_name> <query_file> <output_file>")
      (println "Example: npx @logseq/nbb-logseq extract_blocks_recursive.cljs yihan_main_LOGSEQ queries/wuxian_game_query.edn results/wuxian_game_grouped_content.edn"))
    (let [[graph-name query-file output-file] args
          _ (println (str "Processing graph '" graph-name "', query file '" query-file "', output file '" output-file "'"))
          graph-path (find-graph-path graph-name)]
      (if graph-path
        (let [db (load-graph-db graph-path)]
          (if db
            (process-blocks db query-file output-file)
            (println "Failed to load graph database.")))
        (println "Could not locate graph path.")))))

;; Run the main function with command line arguments
(-main *command-line-args*)
