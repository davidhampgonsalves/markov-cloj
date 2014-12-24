(ns markov-text.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import  (java.util Scanner)
            (clojure.lang PersistentQueue) ))

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

(defn build-chain [scanner prefixCount & {:keys [prefix chain] :or {prefix (PersistentQueue/EMPTY) chain {}}}]
  "returns the next cleaned sentence"
  (if (.hasNext scanner)
    (let [rawWord (.next scanner) word (clean-word rawWord)]
      (if (paragraph-boundry? rawWord)
        (recur scanner prefixCount {:prefix (PersistentQueue/EMPTY) :chain chain})
        (let [prefix (if (> (count prefix) prefixCount) (pop prefix) prefix)
              chain (update-in chain [(build-prefix prefix) word] (fnil inc 0))]
          (recur scanner prefixCount {:prefix (conj prefix word) :chain chain})
       )))
    chain))

(defn calculate-next-state [chain prefix]
  (let [states (seq (get chain (build-prefix prefix)))
        stateCount (reduce (fn [c [state count]] 
                  (+ c count)) 0 states)
        stateIndex (rand-int stateCount)]
      (loop [c 0 i 0]
        (if (nil? (nth states i))
          (do 
            (println "\n\nERROR: states at " i " is nil. states: " states)
            (println "prefix " (build-prefix prefix))))
        (let [state (nth states i) c (+ c (last state))]
          (if (>= c stateIndex)
            (first state)
            (recur c (inc i)))))))

(defn generate-sentence [chain prefixCount & 
        {:keys [sentence prefix] :or {sentence (vector) prefix (PersistentQueue/EMPTY)}}]
    (let [prefix (if (> (count prefix) prefixCount) (pop prefix) prefix)
          word (calculate-next-state chain prefix)]
      (if (last-word? word)
        (conj sentence word)
        (recur chain prefixCount {:sentence (conj sentence word) :prefix (conj prefix word)}))))

(defn create-scanner [fileName]
  (.useDelimiter (new Scanner (io/file fileName)) "([\t ]+)|([\n]+)"))

(defn -main
  "I run the show"
  [& args]
  (let [prefixCount 2
        sc (create-scanner (nth args 0))
        chain (build-chain sc prefixCount)]
    (dotimes [i 3]
      (println (str/capitalize (str/join " " (generate-sentence chain prefixCount)))))))

