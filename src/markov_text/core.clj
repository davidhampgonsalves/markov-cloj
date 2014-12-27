(ns markov-text.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import  (java.util Scanner)
            (clojure.lang PersistentQueue) ))

(def prefix-count 3)


; When paragraph add next work to chain on its own?
; when generating sentences don't store the prefix just take from sentence ending.

(defn clean-word [word]
  (comment "maybe I should match states without some punctuation but leave it in the output")
  (-> word 
    .trim
    (.replaceAll "[\"]+" "")))

(defn conj-prefix [prefix word]
  (conj prefix (str/upper-case word)))

(defn last-word? [word]
  (comment "is last word needs to handle words like N.S.A.")
  (-> word
    .trim
    (.matches ".+[!.?]$")))

(defn prefix-str [prefix]
  "Join prefix into a string"
  (str/upper-case (str/join " " prefix)))

(defn build-chain [scanner & {:keys [prefix chain] :or {prefix (PersistentQueue/EMPTY) chain {}}}]
  "Build a Markov chain based on the provided scanner."
  (if (.hasNext scanner)
    (let [raw-word (.next scanner) word (clean-word raw-word)
          prefix-full (> (count prefix) prefix-count)
          prefix (if prefix-full (pop prefix) prefix)
          chain (if prefix-full (update-in chain [(prefix-str prefix) word] (fnil inc 0)) chain)]
        (recur scanner {:prefix (conj-prefix prefix word) :chain chain}))
    chain))

(defn calculate-next-state [chain prefix]
  (let [states (seq (get chain (prefix-str prefix)))
        state-count (reduce (fn [c [state count]] 
                  (+ c count)) 0 states)
        state-index (rand-int state-count)]
    (loop [c 0 i 0]
      (let [state (nth states i) c (+ c (last state))]
        (if (>= c state-index)
          (first state)
          (recur c (inc i)))))))

(defn initial-state [chain]
  "Randomly select an initial state"
  (let [prefixes (keys chain)
        prefix-index (rand-int (count prefixes))
        prefix (str/split (nth prefixes prefix-index) #" ")]
    (if (-> prefix first (.endsWith "."))
      (into PersistentQueue/EMPTY (flatten [(next prefix) (calculate-next-state chain prefix)]))
      (recur chain))))

(defn generate-sentence [chain prefix & 
        {:keys [sentence] :or {sentence []}}]
    (if (nil? prefix)
      (let [prefix (initial-state chain)] 
        (recur chain prefix {:sentence (into [] prefix)}))
      (let [word (calculate-next-state chain prefix)]
        (if (last-word? word)
          (conj sentence word)
          (recur chain (conj-prefix (pop prefix) word) {:sentence (conj sentence word)})))))

(defn create-scanner [fileName]
  (.useDelimiter (new Scanner (io/file fileName)) "([\t \n]+)"))

(defn -main
  "build markov chain based on supplied input and generate text from them."
  [& args]
  ;;build the chain for each input file in parallel and then merge them
  (let [chains (pmap #(build-chain (create-scanner %)) args)
        chain (apply merge-with #(merge-with + % %2) chains)]
      (loop [i 0 prefix nil]
        (let [sentence (generate-sentence chain prefix)]
          (println (str/join " " sentence))
          (if (< i 3)
            (recur (inc i) (into (PersistentQueue/EMPTY) (take-last prefix-count sentence))))))
      (shutdown-agents)))

