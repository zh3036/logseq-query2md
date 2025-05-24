# Results of Running pyq_references.edn Query

This file contains blocks that explicitly reference the "pyq" page or tag.

## Results

```clojure
([{:block/uuid #uuid "668a36f0-b1da-479c-b888-fcd2c1dd6b91",
   :block/left {:db/id 20524},
   :block/refs [{:db/id 26}],
   :block/format :markdown,
   :block/content "我从语言和理性的边界来探索 非理性的地方\n而不是直接探索\n#pyq",
   :db/id 20521,
   :block/path-refs [{:db/id 26} {:db/id 14912} {:db/id 20515}],
   :block/parent {:db/id 20515},
   :block/page {:db/id 20515}}])
```

## How to Run This Query

```bash
cd /Users/yihan/LocalYihan/pyq_analysis/query_ls
./lq yihan_main_LOGSEQ ./queries/pyq_references.edn
```
