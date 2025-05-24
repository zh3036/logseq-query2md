#!/usr/bin/env bb

(require '[clojure.edn :as edn]
         '[clojure.pprint :as pp])

(defn extract-content [input-file output-file]
  (let [data (-> input-file slurp edn/read-string)
        contents (map (fn [entry]
                        (let [first-entry (first entry)]
                          {:content (:block/content first-entry)
                           :page (when-let [page (:block/page first-entry)]
                                   (:db/id page))}))
                      data)]
    (spit output-file (with-out-str (pp/pprint contents)))))

(when (>= (count *command-line-args*) 2)
  (let [input-file (first *command-line-args*)
        output-file (second *command-line-args*)]
    (extract-content input-file output-file)
    (println "Extracted content saved to:" output-file)))
