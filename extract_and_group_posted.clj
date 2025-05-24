#!/usr/bin/env bb

(require '[clojure.edn :as edn]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(defn read-input-edn [input-file]
  (let [raw-slurp (slurp input-file)
        ;; Look for the line that indicates actual query results are starting
        query-results-marker "Query results:"
        marker-index (str/index-of raw-slurp query-results-marker)]
    (if marker-index
      (let [edn-str-start (+ marker-index (count query-results-marker))
            ;; Find the first actual EDN collection character after the marker
            actual-edn-start-char-index (loop [idx edn-str-start]
                                          (when (< idx (count raw-slurp))
                                            (let [char (nth raw-slurp idx)]
                                              (if (or (= char \#) (= char \() (= char \[
))
                                                idx
                                                (recur (inc idx))))))            
            edn-str (if actual-edn-start-char-index
                      (subs raw-slurp actual-edn-start-char-index)
                      "")] ; Fallback to empty string if no EDN found after marker
        (try
          (edn/read-string edn-str)
          (catch Exception e
            (println (str "Error reading EDN from file: " input-file 
                          ", after marker '" query-results-marker "', content snippet: " 
                          (subs edn-str 0 (min 100 (count edn-str)))))
            (throw e))))
      (do
        (println (str "Warning: Could not find '" query-results-marker "' in " input-file ". Attempting to read as is."))
        (try ; Attempt to read the whole thing if marker not found
          (edn/read-string raw-slurp)
          (catch Exception e
             (println (str "Error reading EDN from file (no marker found): " input-file 
                          ", content snippet: " 
                          (subs raw-slurp 0 (min 100 (count raw-slurp)))))
            (throw e)))))))

(defn transform-block [block]
  (when block
    (let [content (:block/content block)
          page-info (:block/page block)
          ;; Prefer original-name, fallback to db/id if page is just an ID map like {:db/id 123}
          page-name (if (map? page-info)
                      (or (:block/original-name page-info) (str (:db/id page-info)))
                      nil)
          uuid (:block/uuid block)
          children (when-let [child-blocks (:block/children block)]
                     (->> child-blocks
                          (mapv transform-block) ; Recursively transform children
                          (remove nil?)))]
      (cond-> {:content content :uuid uuid}
        page-name (assoc :page page-name)
        (seq children) (assoc :children children)))))

(defn get-all-descendant-uuids-from-pulled-block [pulled-block]
  (when pulled-block
    (lazy-seq
      (let [children (:block/children pulled-block)]
        (concat (map :block/uuid children)
                (mapcat get-all-descendant-uuids-from-pulled-block children))))))

(defn extract-and-group [input-file output-file]
  (let [raw-query-results (read-input-edn input-file)]
    (if (or (nil? raw-query-results) (not (seqable? raw-query-results))) ; Check if it's nil or not a sequence
      (println (str "Error: Parsed EDN data is not a sequence or is nil. Input file: " input-file
                    ". Parsed as: " (pr-str raw-query-results)))
      (let [matched-blocks (mapv first raw-query-results)

        ;; Collect UUIDs of all blocks that appear as a child/descendant of any *matched* block
        ;; This uses the :block/children hierarchy present in the pulled matched_blocks
        uuids_of_descendants_of_matched_blocks
        (->> matched-blocks
             (mapcat get-all-descendant-uuids-from-pulled-block)
             (remove nil?)
             set)]

    ;; A block from matched-blocks is considered a "root" for our output if
    ;; it's not a descendant of another block *also* in matched-blocks.
    (let [root-blocks (filter
                       (fn [block]
                         (not (contains? uuids_of_descendants_of_matched_blocks (:block/uuid block))))
                       matched-blocks)

          transformed-data (->> root-blocks
                                (mapv transform-block) ; transform-block creates the desired output structure
                                (remove nil?))]
          (spit output-file (with-out-str (pp/pprint transformed-data)))) ; Closes the innermost `let`
        ) ; Closes the `let` that defines matched-blocks
      ) ; Closes the `if`'s else branch
    ) ; Closes the main `let` of extract-and-group
  ) ; Closes `defn extract-and-group`

(defn -main []
  (if (< (count *command-line-args*) 2)
    (println "Usage: ./extract_and_group_posted.clj <input_edn_file> <output_edn_file>")
    (let [input-file (first *command-line-args*)
          output-file (second *command-line-args*)]
      (extract-and-group input-file output-file)
      (println "Extracted and grouped content saved to:" output-file))))

(when (= *file* (System/getProperty "babashka.file"))
  (-main))
