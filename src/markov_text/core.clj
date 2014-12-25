(ns markov-text.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import  (java.util Scanner)
            (clojure.lang PersistentQueue) ))

(def prefix-count 2)

(defn clean-word [word]
  (comment "maybe I should match states without some punctuation but leave it in the output")
  (-> word 
    .toLowerCase
    .trim
    (.replaceAll "[,0-9*&:;\"'-]+" "")))

(defn last-word? [word]
  (comment "is last word needs to handle words like N.S.A.")
  (-> word
    .trim
    (.matches ".+[!.?]$")))

(defn paragraph-boundry? [word]
  (.isEmpty word))

(defn build-prefix [prefix]
  (str/join " " prefix))

(defn build-chain [scanner & {:keys [prefix chain] :or {prefix (PersistentQueue/EMPTY) chain {}}}]
  "returns the next cleaned sentence"
  (if (.hasNext scanner)
    (let [raw-word (.next scanner) word (clean-word raw-word)]
      (if (paragraph-boundry? raw-word)
        (recur scanner {:prefix (PersistentQueue/EMPTY) :chain chain})
        (let [prefix (if (> (count prefix) prefix-count) (pop prefix) prefix)
              chain (update-in chain [(build-prefix prefix) word] (fnil inc 0))]
          (recur scanner {:prefix (conj prefix word) :chain chain})
       )))
    chain))

(defn calculate-next-state [chain prefix]
  (let [states (seq (get chain (build-prefix prefix)))
        state-count (reduce (fn [c [state count]] 
                  (+ c count)) 0 states)
        state-index (rand-int state-count)]
      (if (nil? states)
        (do
          (println "\n\nstates: " states)
          (println "prefix " (build-prefix prefix))
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
          (recur chain (conj prefix word) {:sentence (conj sentence word)}))
        nil)))

(defn create-scanner [fileName]
  (.useDelimiter (new Scanner (io/file fileName)) "([\t ]+)|([\n]+)"))

(defn -main
  "I run the show"
  [& args]
  (let [sc (create-scanner (nth args 0))
        chain (build-chain sc)]

      (loop [i 0 prefix (PersistentQueue/EMPTY)]
        (let [sentence (generate-sentence chain prefix)]
          (println (str/capitalize (str/join " " sentence)))
          (if (< i 3)
            (recur (inc i) (into (PersistentQueue/EMPTY) (take-last prefix-count sentence))))))))

