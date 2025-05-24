tags:: [[logseq/query usage]]
-
- 筛选deadline和schedule
  #+BEGIN_QUERY
  {:title "next week deadline or schedule"
      :query [:find (pull ?block [*])
              :in $ ?start ?next
              :where
              (or
                [?block :block/scheduled ?d]
                [?block :block/deadline ?d])
              [(> ?d ?start)]
              [(< ?d ?next)]]
      :inputs [:today :7d-after]
      :collapsed? false}
  #+END_QUERY
- samples from documents https://logseq.github.io/#/page/advanced%20queries
	- **Examples**
		- 1. Get all tasks
		   created_at:: 1609232063516
		   updated-at:: 1609245970090
		   ``` clojure
		   #+BEGIN_QUERY
		   {:title "All tasks"
		   :query [:find (pull ?b [*])
		   :where
		   [?b :block/marker ?m]
		   [(not= ?m "nil")]]}
		   #+END_QUERY
		   ```
		- 2. Get all tasks with a tag "project"
		   ``` clojure
		   #+BEGIN_QUERY
		   {:title "All todos with tag project"
		   :query [:find (pull ?b [*])
		   :where
		   [?p :block/name "project"]
		   [?b :block/ref-pages ?p]]}
		   #+END_QUERY
		   ```
		- 3. Blocks in 7ds with a page reference of datalog
		  ``` clojure
		  	  
		  	  #+BEGIN_QUERY
		  	  {:title "Blocks in 7ds with a page reference of datalog"
		  	   :query [:find (pull ?b [*])
		  	         :in $ ?start ?today ?tag
		  	         :where
		  	         [?b :block/page ?p]
		  	         [?p :page/journal-day ?d]
		  	         [(>= ?d ?start)]
		  	         [(<= ?d ?today)]
		  	         [?b :block/ref-pages ?rp]
		  	         [?rp :block/name ?tag]]
		  	   :inputs [:7d-before :today "datalog"]}
		  	  #+END_QUERY
		  ```
		- 4. All TODOs
		  ``` clojure
		  #+BEGIN_QUERY
		  {:title "TODO"
		   :query [:find (pull ?b [*])
		         :where
		         [?b :block/marker ?marker]
		         [(= "TODO" ?marker)]]}
		  #+END_QUERY
		  ```
		- 5. All the tags specified in the front matter (tags: tag1, tag2)
		  ``` clojure
		  #+BEGIN_QUERY
		  {:title "All page tags"
		  :query [:find ?tag-name
		        :where
		        [?tag :block/name ?tag-name]]
		  :view (fn [tags]
		        [:div
		         (for [tag (flatten tags)]
		           [:a.tag.mr-1 {:href (str "/page/" tag)}
		            (str "#" tag)])])}
		  #+END_QUERY
		  ```
		- 6. All pages have a "programming" tag
		  ``` clojure
		  #+BEGIN_QUERY
		  {:title "All pages have a *programming* tag"
		   :query [:find ?name
		         :in $ ?tag
		         :where
		         [?t :block/name ?tag]
		         [?p :page/tags ?t]
		         [?p :block/name ?name]]
		   :inputs ["programming"]
		   :view (fn [result]
		         [:div.flex.flex-col
		          (for [page result]
		            [:a {:href (str "/page/" page)} (clojure.string/capitalize page)])])}
		  #+END_QUERY
		  ```
		- 7. Get all the blocks with the property "type" and the value "programming_lang"
		  ``` clojure
		  	  
		  	  #+BEGIN_QUERY
		  	  {:title [:h2 "Programming languages list"]
		  	   :query [:find (pull ?b [*])
		  	         :where
		  	         [?b :block/properties ?p]
		  	         [(get ?p :type) ?t]
		  	         [(= "programming_lang" ?t)]]
		  	   }
		  	  #+END_QUERY
		  ```
		- 8. All todos tagged using current page
		  ``` clojure
		  	  
		  	  #+BEGIN_QUERY
		  	  {:title "All todos tagged using current page"
		  	   :query [:find (pull ?b [*])
		  	         :in $ ?current-page
		  	         :where
		  	         [?p :block/name ?current-page]
		  	         [?b :block/marker ?marker]
		  	         [?b :block/ref-pages ?p]
		  	         [(= "TODO" ?marker)]]
		  	   :inputs [:current-page]}
		  	  #+END_QUERY
		  ```
		- 9. Tasks made active in the last 2 weeks
		  ``` clojure
		  	  
		  	  #+BEGIN_QUERY
		  	  {:title "🟢 ACTIVE"
		  	    :query [:find (pull ?h [*])
		  	            :in $ ?start ?today
		  	            :where
		  	            [?h :block/marker ?marker]
		  	            [?h :block/page ?p]
		  	            [?p :page/journal? true]
		  	            [?p :page/journal-day ?d]
		  	            [(>= ?d ?start)]
		  	            [(<= ?d ?today)]
		  	            [(contains? #{"NOW" "DOING"} ?marker)]]
		  	    :inputs [:14d :today]
		  	    :result-transform (fn [result]
		  	                        (sort-by (fn [h]
		  	                                   (get h :block/priority "Z")) result))
		  	    :collapsed? false}
		  	  #+END_QUERY
		  ```
		- 10. Tasks referencing due dates in the past
		  ``` clojure
		  	  
		  	  #+BEGIN_QUERY
		  	   {:title "⚠️ OVERDUE"
		  	    :query [:find (pull ?h [*])
		  	            :in $ ?start ?today
		  	            :where
		  	            [?h :block/marker ?marker]
		  	            [?h :block/ref-pages ?p]
		  	            [?p :page/journal? true]
		  	            [?p :page/journal-day ?d]
		  	            [(>= ?d ?start)]
		  	            [(<= ?d ?today)]
		  	            [(contains? #{"NOW" "LATER" "TODO" "DOING"} ?marker)]]
		  	    :inputs [:56d :today]
		  	    :collapsed? false}
		  	  #+END_QUERY
		  ```
		- 11. Tasks referencing due dates up to 10 days ahead
		  ``` clojure
		  	  
		  	  #+BEGIN_QUERY
		  	      {:title "📅 NEXT"
		  	    :query [:find (pull ?h [*])
		  	            :in $ ?start ?next
		  	            :where
		  	            [?h :block/marker ?marker]
		  	            [?h :block/ref-pages ?p]
		  	            [?p :page/journal? true]
		  	            [?p :page/journal-day ?d]
		  	            [(> ?d ?start)]
		  	            [(< ?d ?next)]
		  	            [(contains? #{"NOW" "LATER" "DOING" "TODO"} ?marker)]]
		  	    :inputs [:today :10d-after]
		  	    :collapsed? false}
		  	  #+END_QUERY
		  ```
		- 12. Tasks from last week which are still outstanding (may slip soon!)
		  ``` clojure
		  	  
		  	  #+BEGIN_QUERY
		  	     {:title "🟠 SLIPPING"
		  	    :query [:find (pull ?h [*])
		  	            :in $ ?start ?today
		  	            :where
		  	            [?h :block/marker ?marker]
		  	            [?h :block/page ?p]
		  	            [?p :page/journal? true]
		  	            [?p :page/journal-day ?d]
		  	            [(>= ?d ?start)]
		  	            [(<= ?d ?today)]
		  	            [(contains? #{"NOW" "LATER" "TODO" "DOING"} ?marker)]]
		  	    :inputs [:7d :today]
		  	    :result-transform (fn [result]
		  	                        (sort-by (fn [h]
		  	                                   (get h :block/created-at)) result))
		  	    :collapsed? true}
		  	  #+END_QUERY
		  ```
		- 13. Tasks created more than 1 week ago, less old than 2 months but still outstanding
		  ``` clojure
		  	  
		  	  #+BEGIN_QUERY
		  	  {:title "🔴 STALLED"
		  	    :query [:find (pull ?h [*])
		  	            :in $ ?start ?today
		  	            :where
		  	            [?h :block/marker ?marker]
		  	            [?h :block/page ?p]
		  	            [?p :page/journal? true]
		  	            [?p :page/journal-day ?d]
		  	            [(>= ?d ?start)]
		  	            [(<= ?d ?today)]
		  	            [(contains? #{"NOW" "LATER" "TODO" "DOING"} ?marker)]]
		  	    :inputs [:56d :8d]
		  	    :result-transform (fn [result]
		  	                        (sort-by (fn [h]
		  	                                   (get h :block/created-at)) result))
		  	    :collapsed? true}
		  	   ]}
		  	  #+END_QUERY
		  ```
		- 14. Next 7 days' deadline or schedule
		  		* 1. Add ~Deadline~ and ~Scheduled~ support!
		  
		  *How to use it?*
		  		1. Type ~/deadline~ or ~/scheduled~ in the block editor.
		  		2. Pick a date, time (optional), and a repeater (optional, see below).
		  		3. Click the ~Submit~ button.
		  
		  [[https://cdn.logseq.com/%2F8b9a461d-437e-4ca5-a2da-18b51077b5142020_10_23_Screenshot%202020-10-23%2020-36-43%20%2B0800.png?Expires=4757056622&Signature=mOSq9~NdKi5UpGsnuf5RH7VpwrY14l56ouPHCYcZ-TyNvOYE2OJ-Je0fT29AtODAyAmpz0U0sOBk147kT1hkjaBur6KRq5NXXRz8plSai8xGxNRIxuCgtw32E0xsE-nJ8BOTd9wfmXJXPAqEIpfDNI5XOLpmnogv4aflG1BiUPzD6Ap815Sss6kP6qozV0lBtihJha1Vj3yGJbMtjJfpuIuqwstse2Cac6icLt5oiFyjNTsHM3kwbRAXl37oyJCb9-tBU~RYruGvp3FrwvJZYAPqAQhFR69XHqdk54GNCE-sY5xGN0nwA6fjZKJoHTGKxkyUZT7VINPz~ORtdpwQqA__&Key-Pair-Id=APKAJE5CCD6X7MP6PTEA][2020_10_23_Screenshot 2020-10-23 20-36-43 +0800.png]]
		  ``` clojure
		  #+BEGIN_QUERY
		  {:title "next 7 days' deadline or schedule"
		    :query [:find (pull ?block [*])
		            :in $ ?start ?next
		            :where
		            (or
		              [?block :block/scheduled ?d]
		              [?block :block/deadline ?d])
		            [(> ?d ?start)]
		            [(< ?d ?next)]]
		    :inputs [:today :7d-after]
		    :collapsed? false}
		  #+END_QUERY
		  ```