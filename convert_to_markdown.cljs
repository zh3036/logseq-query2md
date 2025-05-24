#!/usr/bin/env nbb

(ns convert-to-markdown
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.walk :as walk]
            ["fs" :as fs]))

(defn escape-markdown [text]
  "Escape special markdown characters in text"
  (when text
    (-> text
        (str/replace #"\*" "\\*")
        (str/replace #"_" "\\_")
        (str/replace #"`" "\\`")
        (str/replace #"#" "\\#")
        (str/replace #"\[" "\\[")
        (str/replace #"\]" "\\]"))))

(defn extract-title [content]
  "Extract a title from the content, using the first sentence or first 50 chars"
  (when content
    (let [content-str (str content)
          first-line (first (str/split-lines content-str))
          cleaned (str/replace first-line #"\[\[.*?\]\]" "")
          cleaned (str/replace cleaned #"#\w+" "")
          cleaned (str/trim cleaned)]
      (if (> (count cleaned) 50)
        (str (subs cleaned 0 47) "...")
        cleaned))))

(defn format-content [content level]
  "Format content with proper markdown escaping and structure"
  (when-not (str/blank? content)
    (let [escaped (escape-markdown content)
          lines (str/split-lines escaped)]
      (if (= level 1)
        ;; For top level, don't indent
        (str/join "\n" lines)
        ;; For nested levels, add some indentation for readability
        (str/join "\n" (map #(str "  " %) lines))))))

(defn blocks-to-markdown 
  ([blocks] (blocks-to-markdown blocks 1))
  ([blocks level]
   (when (seq blocks)
     (str/join "\n\n" 
               (map (fn [block]
                      (let [content (:content block)
                            page (:page block)
                            uuid (:uuid block)
                            children (:children block)
                            title (extract-title content)
                            header-level (min level 6)] ; Markdown only supports up to 6 levels
                        
                        (str
                          ;; Create markdown header
                          (str (str/join "" (repeat header-level "#")) " " 
                               (if (str/blank? title) 
                                 (str "Block from " page) 
                                 title)) "\n\n"
                          
                          ;; Add content
                          (when-not (str/blank? content)
                            (str (format-content content level) "\n\n"))
                          
                          ;; Add children recursively
                          (when (seq children)
                            (str "### Children\n\n"
                                 (blocks-to-markdown children (inc level)))))))
                    blocks)))))

(defn convert-edn-to-markdown [input-file output-file]
  "Convert EDN file to markdown format"
  (try
    (println (str "Reading EDN file: " input-file))
    (let [edn-content (fs/readFileSync input-file "utf8")
          blocks (edn/read-string edn-content)
          markdown (str "# PyQ Posted Content Analysis\n\n"
                       "This document contains the hierarchical content extracted from Logseq blocks.\n\n"
                       "---\n\n"
                       (blocks-to-markdown blocks))]
      
      (println (str "Writing markdown to: " output-file))
      (fs/writeFileSync output-file markdown)
      (println "Conversion completed successfully!"))
    
    (catch js/Error e
      (println (str "Error during conversion: " (.-message e))))))

;; Get command line arguments from process.argv
(let [all-args (js->clj js/process.argv)
      script-args (->> all-args
                       (drop-while #(not= % "--"))
                       (drop 1))]
  (if (= (count script-args) 2)
    (let [[input-file output-file] script-args]
      (convert-edn-to-markdown input-file output-file))
    (do
      (println "Usage: nbb convert_to_markdown.cljs -- <input-edn-file> <output-markdown-file>")
      (println "Example: nbb convert_to_markdown.cljs -- query_ls/results/pyq_posted_optimized_output.edn pyq_analysis.md")))) 