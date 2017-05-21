(ns etl-pipeline.core
  (:require
   [planck.core :refer [IBufferedReader line-seq with-open -read-line -close]]
   [planck.io :as io]))

(defn create-file []
  (letfn [(rand-obj []
            (case (rand-int 3)
              0 #js {:type "number" :number (rand-int 1000)}
              1 #js {:type "string" :string (apply str (repeatedly 30 #(char (+ 33 (rand-int 90)))))}
              2 #js {:type "empty"}))]
    (with-open [writer (io/writer "/tmp/dummy.json")]
      (dotimes [_ 1000000]
        (-write writer (js/JSON.stringify (rand-obj)))
        (-write writer \newline)))))

(defn decode [o keywordize-keys]
  (js->clj (js/JSON.parse o) :keywordize-keys keywordize-keys))

(defn parse-json-file-lazy [file]
  (map #(decode % true)
    (line-seq (io/reader file))))

(defn valid-entry? [log-entry]
  (not= (:type log-entry) "empty"))

(defn transform-entry-if-relevant [log-entry]
  (cond (= (:type log-entry) "number")
        (let [number (:number log-entry)]
          (when (> number 900)
            (assoc log-entry :number (Math/log number))))

        (= (:type log-entry) "string")
        (let [string (:string log-entry)]
          (when (re-find #"a" string)
            (update log-entry :string str "-improved!")))))

(def db (atom 0))                                           ; Dummy "database"

(defn save-into-database [batch]
  (swap! db + (count batch)))                               ; Simulate inserting data into database.

(defn process [files]
  (->> files
    (mapcat parse-json-file-lazy)                           ; mapcat b/c one file produces many entries
    (filter valid-entry?)
    (keep transform-entry-if-relevant)
    (partition-all 1000)                                    ; Form batches for saving into database.
    (map save-into-database)
    doall))                                                 ; Force eagerness at the end.

(defn lines-reducible [^IBufferedReader rdr]
  (reify IReduce
    (-reduce [this f init]
      (try
        (loop [state init]
          (if (reduced? state)
            state
            (if-let [line (-read-line rdr)]
              (recur (f state line))
              state)))
        (finally (-close rdr))))))


(defn parse-json-file-reducible [file]
  (eduction (map #(decode % true))
    (lines-reducible (io/reader file))))

(defn process-with-transducers [files]
  (transduce (comp (mapcat parse-json-file-reducible)
               (filter valid-entry?)
               (keep transform-entry-if-relevant)
               (partition-all 1000)
               (map save-into-database))
    (constantly nil)
    nil
    files))
