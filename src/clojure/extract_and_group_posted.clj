#!/usr/bin/env bb

(require '[clojure.edn :as edn]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         '[babashka.process :as p])

;; Dynamic bindings for configuration
(def ^:dynamic *graph-name* nil) ; Will be bound with graph name from CLI args
(def ^:dynamic *fetch-count* (atom 0)) ; Counter for progress reporting

(defn robust-read-edn 
  "Safely parse EDN string with error handling"
  [edn-string source-description]
  (try
    (edn/read-string edn-string)
    (catch Exception e
      (println (str "Error parsing EDN from: " source-description 
                   "\nError: " (.getMessage e) 
                   "\nString snippet: " (subs edn-string 0 (min 200 (count edn-string)))))
      nil)))

(defn run-logseq-query 
  "Run a Logseq query via the lq script, creating a temporary file for the query"
  [query-str]
  (let [tmp-query-file (java.io.File/createTempFile "temp_ls_query_" ".edn")]
    (try
      (spit tmp-query-file query-str)
      (let [cmd ["./query_ls/lq" *graph-name* (.getAbsolutePath tmp-query-file)]
            _ (println (str "Running query: " (apply str (take 60 query-str)) 
                           (when (> (count query-str) 60) "...")))
            process-result (p/sh cmd) ; Assumes running from pyq_analysis dir
            raw-output (:out process-result)]
        (if (not (zero? (:exit process-result)))
          (do 
            (println (str "Error running query. Exit code: " (:exit process-result) 
                         "\nStderr: " (:err process-result)))
            nil)
          (let [query-results-marker "Query results:"
                marker-index (str/index-of raw-output query-results-marker)]
            (if marker-index
              (let [edn-str-start (+ marker-index (count query-results-marker))
                    ;; Find the first actual EDN collection character after the marker
                    actual-edn-start-char-index 
                    (loop [idx edn-str-start]
                      (when (< idx (count raw-output))
                        (let [char (nth raw-output idx)]
                          (if (or (= char \#) (= char \() (= char \[) (= char \{))
                            idx
                            (recur (inc idx))))))
                    edn-str (if actual-edn-start-char-index
                              (subs raw-output actual-edn-start-char-index)
                              "")]
                (if (str/blank? edn-str)
                  []  ; Return empty vector if no EDN content after marker
                  (robust-read-edn edn-str (str "query: " query-str))))
              (do
                (println (str "Warning: No 'Query results:' marker in output"))
                nil)))))
      (finally
        (.delete tmp-query-file)))))

(defn fetch-children-recursively 
  "Recursively fetch all children of a block using its db/id"
  [parent-db-id level]
  (let [query-str (format "[:find (pull ?c [:db/id :block/content :block/uuid {:block/page [:block/original-name :db/id]}]) :where [?c :block/parent %d]]" parent-db-id)
        indent (apply str (repeat level "  "))
        _ (println (str indent "Fetching children for block ID: " parent-db-id))
        child-block-basics (run-logseq-query query-str)]
    
    (when (seq child-block-basics)
      (swap! *fetch-count* + (count child-block-basics))
      (println (str indent "Found " (count child-block-basics) " children, total processed: " @*fetch-count*))
      
      (->> child-block-basics
           (mapv (fn [child-vec] ; child-vec is like [{child-map}]
                   (let [child-map (first child-vec)
                         _ (when (:block/content child-map)
                             (println (str indent "  Child block: " 
                                           (subs (:block/content child-map) 0 
                                                 (min 30 (count (:block/content child-map))))
                                           (when (> (count (:block/content child-map)) 30) "..."))))
                         grand-children (fetch-children-recursively (:db/id child-map) (inc level))]
                     (cond-> {:content (:block/content child-map)
                              :uuid (:block/uuid child-map)
                              :page (when-let [p (:block/page child-map)]
                                      (or (:block/original-name p) (:db/id p)))}
                       (seq grand-children) (assoc :children grand-children)))))
           (remove nil?)))))

(defn process-root-block 
  "Process a root block and all its children recursively"
  [root-block-map block-number total-blocks]
  (println (str "\nProcessing root block " block-number "/" total-blocks ": "
                (subs (:block/content root-block-map) 0 
                      (min 40 (count (:block/content root-block-map))))
                (when (> (count (:block/content root-block-map)) 40) "...")
                " (ID: " (:db/id root-block-map) ")"))
  
  (let [children (fetch-children-recursively (:db/id root-block-map) 1)]
    (cond-> {:content (:block/content root-block-map)
             :uuid (:block/uuid root-block-map)
             :page (when-let [p (:block/page root-block-map)]
                     (or (:block/original-name p) (:db/id p)))}
      (seq children) (assoc :children children))))

(defn extract-and-group 
  "Main function to extract blocks referencing [[pyq/posted]] and group them with their children"
  [graph-name-arg input-file output-file]
  (binding [*graph-name* graph-name-arg
            *fetch-count* (atom 0)]
    (println (str "\n==== Starting extraction with graph: " graph-name-arg " ===="))
    (println (str "Reading input file: " input-file))
    
    (let [raw-input-slurp (slurp input-file)
          query-results-marker "Query results:"
          marker-index (str/index-of raw-input-slurp query-results-marker)
          _ (println "Parsing query results...")
          root-blocks-raw 
          (if marker-index
            (let [edn-str-start (+ marker-index (count query-results-marker))
                  actual-edn-start-char-index 
                  (loop [idx edn-str-start]
                    (when (< idx (count raw-input-slurp))
                      (let [char (nth raw-input-slurp idx)]
                        (if (or (= char \#) (= char \() (= char \[) (= char \{))
                          idx
                          (recur (inc idx))))))
                  edn-str (if actual-edn-start-char-index
                            (subs raw-input-slurp actual-edn-start-char-index)
                            "")]
              (if (str/blank? edn-str) 
                [] 
                (robust-read-edn edn-str (str "input file: " input-file))))
            (do (println (str "Warning: Could not find 'Query results:' in " input-file))
                []))
          
          ;; Each item in root-blocks-raw is like [{map-with-db-id-etc}]
          root-block-maps (if (seq root-blocks-raw) 
                           (mapv first root-blocks-raw) 
                           [])
                           
          _ (println (str "Found " (count root-block-maps) " root blocks referencing [[pyq/posted]]"))
          _ (println "Beginning recursive fetch of all children...")
          
          transformed-data 
          (if (seq root-block-maps)
            (->> root-block-maps
                 (map-indexed (fn [idx block-map]
                                (process-root-block block-map (inc idx) (count root-block-maps))))
                 (remove nil?))
            [])]
      
      (println (str "\n==== Processing complete ===="))
      (println (str "Total blocks processed: " @*fetch-count* 
                   " (includes " (count root-block-maps) " root blocks and their descendants)"))
      
      (if (seq transformed-data)
        (do
          (println (str "Writing results to: " output-file))
          (spit output-file (with-out-str (pp/pprint transformed-data)))
          (println "Write successful."))
        (do
          (println "No data found. Writing empty array.")
          (spit output-file "[]")))
      
      transformed-data))) ; Return for potential use or testing

(defn -main []
  (if (< (count *command-line-args*) 3)
    ;; Not enough arguments
    (do
      (println "Usage: ./extract_and_group_posted.clj <graph_name> <input_edn_file> <output_edn_file>")
      (println "Example: ./extract_and_group_posted.clj yihan_main_LOGSEQ ./query_ls/results/pyq_posted_raw_output.edn ./query_ls/results/pyq_posted_grouped_content.edn"))
    ;; Enough arguments, process them
    (let [[graph-name-arg input-file output-file] *command-line-args*]
      (println (str "Processing graph '" graph-name-arg "', input file '" input-file "', output file '" output-file "'"))
      (let [results (extract-and-group graph-name-arg input-file output-file)]
        (if (seq results)
          (println "Extracted and grouped content saved to:" output-file)
          (println (str "No data processed or no root blocks found. Output file contains []. Check " input-file " and query.")))))))

(when (= *file* (System/getProperty "babashka.file"))
  (-main))
