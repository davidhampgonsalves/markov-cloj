(ns markov-text.core
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:import  (java.util Scanner) ))

(defn cleanWord [word]
  (-> word 
    .toLowerCase
    .trim
    (.replaceAll "[,!-0-9*&.:;]+" "")))

(defn isLastWord [word]
  (comment "is last word needs to handle words like N.S.A.")
  (-> word
    .trim
    (.matches "[!.?]$")))

(defn createQueue []
  (conj (clojure.lang.PersistentQueue/EMPTY) "."))

(defn addToChain [chain prefix word]
  (if-not (contains? @chain prefix)
    (dosync
      (alter chain assoc prefix (atom {word (atom 1)})))
    (let [prefixChain (get @chain prefix)]
      (if-not (contains? @prefixChain word)
        (swap! prefixChain (assoc @prefixChain word (atom 1)))
        (let [wordCount (get @prefixChain word)]
          (swap! wordCount inc))))))

(defn nextSentence [scanner & {:keys [prefix chain] :or {prefix (createQueue) chain (ref {})}}]
  "returns the next cleaned sentence"
  (if (.hasNext scanner)
    (let [rawWord (.next scanner) word (cleanWord rawWord)]
      (if (.isEmpty word)
        (recur scanner {:prefix prefix :chain chain})
        (let [prefix (if (> (.size prefix) 2) (pop prefix) prefix)]
          (addToChain chain (clojure.string/join " " prefix) word)
          (recur scanner {:prefix (if (isLastWord rawWord) (createQueue) (conj prefix word)) :chain chain})
        )))
    @chain))


(defn train [fileName chain]
  (let [sc (new Scanner (io/file fileName)) prefix (vector)]
    (let [chain (nextSentence sc)]
      (println "chain size " (.size chain))
      (println "sentence beginings " (get chain ""))
      (println (keys chain))
      (for [prefix (keys chain)]
        (println prefix))
      )))

(defn -main
  "I run the show"
  [& args]
  (let [chain (ref hash-map)]
    (train (nth args 0) chain)))

