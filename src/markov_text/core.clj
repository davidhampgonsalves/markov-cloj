(ns markov-text.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import  (java.util Scanner)
            (clojure.lang PersistentQueue) ))

(def prefix-count 2)


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

(defn paragraph-boundry? [word]
  (.isEmpty word))

(defn prefix-str [prefix]
  (str/join " " prefix))

(defn build-chain [scanner & {:keys [prefix chain] :or {prefix (PersistentQueue/EMPTY) chain {}}}]
  "returns the next cleaned sentence"
  (if (.hasNext scanner)
    (let [raw-word (.next scanner) word (clean-word raw-word)]
      (if (paragraph-boundry? raw-word)
        (recur scanner {:prefix (PersistentQueue/EMPTY) :chain chain})
        (let [prefix (if (> (count prefix) prefix-count) (pop prefix) prefix)
              chain (update-in chain [(prefix-str prefix) word] (fnil inc 0))]
          (recur scanner {:prefix (conj-prefix prefix word) :chain chain})
       )))
    chain))

(defn calculate-next-state [chain prefix]
  (let [states (seq (get chain (prefix-str prefix)))
        state-count (reduce (fn [c [state count]] 
                  (+ c count)) 0 states)
        state-index (rand-int state-count)]
      (if (nil? states)
        (do
          (println "\n\nstates: " states)
          (println "prefix " (prefix-str prefix))
          nil)
        (loop [c 0 i 0]
          (let [state (nth states i) c (+ c (last state))]
            (if (>= c state-index)
              (first state)
              (recur c (inc i))))))))

(defn generate-sentence [chain prefix & 
        {:keys [sentence] :or {sentence []}}]
    (let [prefix (if (> (count prefix) prefix-count) (pop prefix) prefix)
          word (calculate-next-state chain prefix)]
      (if-not (nil? word)
        (if (last-word? word)
          (conj sentence word)
          (recur chain (conj-prefix prefix word) {:sentence (conj sentence word)}))
        nil)))

(defn create-scanner [fileName]
  (.useDelimiter (new Scanner (io/file fileName)) "([\t ]+)|([\n]+)"))

(defn -main
  "build markov chain based on supplied input and generate text from them."
  [& args]
  ;;build the chain for each input file in parallel and then merge them
  (let [chains (pmap #(build-chain (create-scanner %)) args)
        chain (apply merge-with #(merge-with + % %2) chains)]
      (loop [i 0 prefix (PersistentQueue/EMPTY)]
        (let [sentence (generate-sentence chain prefix)]
          (println (str/join " " sentence))
          (if (< i 3)
            (recur (inc i) (into (PersistentQueue/EMPTY) (take-last prefix-count sentence))))))
      (shutdown-agents)))

