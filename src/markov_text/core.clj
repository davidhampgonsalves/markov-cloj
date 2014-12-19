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
    (.matches ".+[!.?]$")))

(defn isParagraphBoundry [word]
  (.isEmpty word))

(defn createQueue []
  (conj (clojure.lang.PersistentQueue/EMPTY) ""))

(defn addToChain [chain prefix word]
  (println prefix " -> " word)
  (if-not (contains? @chain prefix)
    (dosync
      (alter chain assoc prefix (ref {word (atom 1)})))
    (let [prefixChain (get @chain prefix)]
      (if-not (contains? @prefixChain word)
        (dosync (alter prefixChain assoc word (atom 1)))
        (let [wordCount (get @prefixChain word)]
          (swap! wordCount inc))))))

(defn buildChain [scanner & {:keys [prefix chain] :or {prefix (createQueue) chain (ref {})}}]
  "returns the next cleaned sentence"
  (if (.hasNext scanner)
    (let [rawWord (.next scanner) word (cleanWord rawWord)]
      (comment ":: " rawWord " - " (isParagraphBoundry rawWord) " - " (count rawWord))
      (if (isParagraphBoundry rawWord)
         (do
          (println "> PARAGRAPH BOUNDRY")
          (comment "handle pharagraphs by clearing prefix queue")
          (recur scanner {:prefix (createQueue) :chain chain}))
        (let [prefix (if (> (count prefix) 2) (pop prefix) prefix)]
          (addToChain chain (clojure.string/join " " prefix) word)
          (recur scanner {:prefix (conj prefix word) :chain chain})
       )))
    @chain))


(defn train [fileName chain]
  (let [sc (.useDelimiter (new Scanner (io/file fileName)) "([\t ]+)|([\n])") prefix (vector)]
    (let [chain (buildChain sc)]
      (println "chain size " (count chain))
      (println "sentence beginings " (deref (get chain "")))
      (loop [k (keys chain)]
        (println ">")
        (println k))
      )))

(defn -main
  "I run the show"
  [& args]
  (let [chain (ref hash-map)]
    (train (nth args 0) chain)))

