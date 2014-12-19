(ns markov-text.core
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:import  (java.util Scanner) ))

(defn cleanWord [word]
  (-> word 
       .toLowerCase
       .trim))

(defn isLastWord [word]
  (comment "is last word needs to handle words like N.S.A.")
  (.endsWith word "."))

(defn createQueue []
  (conj (clojure.lang.PersistentQueue/EMPTY) "."))

(defn nextSentence [scanner & {:keys [prefix chain] :or {prefix (createQueue) chain (hash-map)}}]
  "returns the next cleaned sentence"
  (if (.hasNext scanner)
    (let [prefix (if (> (.size prefix) 2) (pop prefix) prefix)]
      (let [w (.next scanner) prefix (conj prefix (cleanWord w))]
        (println (clojure.string/join " " prefix))
        (recur scanner {:prefix (if (isLastWord w) (createQueue) prefix) :chain chain})
        ))
    chain))


(defn train [fileName chain]
  (let [sc (new Scanner (io/file fileName)) prefix (vector)]
    (print (nextSentence sc))))

(defn -main
  "I run the show"
  [& args]
  (let [chain (ref hash-map)]
    (train (nth args 0) chain)))

