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

(defn generate-toc [blocks]
  "Generate table of contents from top-level blocks"
  (when (seq blocks)
    (str "## Table of Contents\n\n"
         (str/join "\n" 
                   (map-indexed 
                     (fn [idx block]
                       (let [title (extract-title (:content block))
                             display-title (if (str/blank? title) 
                                             (str "Block from " (:page block))
                                             title)
                             anchor (str "block-" (inc idx))]
                         (str (inc idx) ". [" display-title "](#" anchor ")")))
                     blocks))
         "\n\n---\n\n")))

(defn format-list-content [content level]
  "Format content for list items with proper indentation"
  (when-not (str/blank? content)
    (let [escaped (escape-markdown content)
          lines (str/split-lines escaped)
          indent (str/join "" (repeat (* (dec level) 2) " "))]
      (str/join (str "\n" indent "  ") lines))))

(defn children-to-list [children level]
  "Convert children blocks to nested markdown lists"
  (when (seq children)
    (str/join "\n"
              (map (fn [child]
                     (let [content (:content child)
                           child-children (:children child)
                           indent (str/join "" (repeat (* (dec level) 2) " "))
                           bullet (if (odd? level) "-" "*")]
                       (str indent bullet " " 
                            (if (str/blank? content)
                              "*(empty block)*"
                              (format-list-content content level))
                            (when (seq child-children)
                              (str "\n" (children-to-list child-children (inc level)))))))
                   children))))

(defn blocks-to-markdown [blocks]
  "Convert blocks to markdown with TOC and list structure"
  (when (seq blocks)
    (str (generate-toc blocks)
         (str/join "\n\n" 
                   (map-indexed (fn [idx block]
                                  (let [content (:content block)
                                        children (:children block)
                                        title (extract-title content)
                                        display-title (if (str/blank? title) 
                                                        (str "Block from " (:page block)) 
                                                        title)
                                        anchor (str "block-" (inc idx))]
                                    (str
                                      ;; Top-level header with anchor
                                      "# " display-title " {#" anchor "}\n\n"
                                      
                                      ;; Top-level content
                                      (when-not (str/blank? content)
                                        (str (escape-markdown content) "\n\n"))
                                      
                                      ;; Children as nested lists
                                      (when (seq children)
                                        (children-to-list children 1)))))
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