(ns markov-text.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import  (java.util Scanner)
            (clojure.lang PersistentQueue) ))

(defn clean-word [word]
  (-> word 
    .toLowerCase
    .trim
    (.replaceAll "[,0-9*&:;\"'-]+" ""))
  word)

(defn last-word? [word]
  (comment "is last word needs to handle words like N.S.A.")
  (-> word
    .trim
    (.matches ".+[!.?]$")))

(defn isParagraphBoundry [word]
  (.isEmpty word))

(defn build-prefix [prefix]
  (str/join " " prefix))

(defn build-chain [scanner & {:keys [prefix chain] :or {prefix (PersistentQueue/EMPTY) chain {}}}]
  "returns the next cleaned sentence"
  (if (.hasNext scanner)
    (let [rawWord (.next scanner) word (clean-word rawWord)]
      (if (isParagraphBoundry rawWord)
        (recur scanner {:prefix (PersistentQueue/EMPTY) :chain chain})
        (let [prefix (if (> (count prefix) 2) (pop prefix) prefix)
              chain (update-in chain [(build-prefix prefix) word] (fnil inc 0))]
          (recur scanner {:prefix (conj prefix word) :chain chain})
       )))
    chain))

(defn calculate-next-state [chain prefix]
  (let [states (seq (get chain (build-prefix prefix)))
        stateCount (reduce (fn [c [state count]] 
                  (+ c count)) 0 states)
        stateIndex (rand-int stateCount)]
      (loop [c 0 i 0]
        (let [state (nth states i) c (+ c (last state))]
          (if (>= c stateIndex)
            (first state)
            (recur c (inc i)))))))

(defn generate-sentence [chain & {:keys [sentence prefix] :or {sentence (vector) prefix (PersistentQueue/EMPTY)}}]
    (let [prefix (if (> (count prefix) 2) (pop prefix) prefix)
          word (calculate-next-state chain prefix)]
      (if (last-word? word)
        (conj sentence word)
        (recur chain {:sentence (conj sentence word) :prefix (conj prefix word)}))))

(defn train [fileName chain]
  (let [sc (.useDelimiter (new Scanner (io/file fileName)) "([\t ]+)|([\n])") prefix (vector)]
    (let [chain (build-chain sc)]
      (println "chain size " (count chain))
      (println "need to walk chain to generate sentence")
      (println (generate-sentence chain) )
      )))

(defn -main
  "I run the show"
  [& args]
  (let [chain (ref hash-map)]
    (train (nth args 0) chain)))

