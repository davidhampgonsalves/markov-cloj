(ns markov-text.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import  (java.util Scanner) ))

(defn cleanWord [word]
  (-> word 
    .toLowerCase
    .trim
    (.replaceAll "[,0-9*&:;\"'-]+" ""))
  word)

(defn isLastWord [word]
  (comment "is last word needs to handle words like N.S.A.")
  (-> word
    .trim
    (.matches ".+[!.?]$")))

(defn isParagraphBoundry [word]
  (.isEmpty word))

(defn createQueue []
  (clojure.lang.PersistentQueue/EMPTY))

(defn build-prefix [prefix]
  (str/join " " prefix))

(defn buildChain [scanner & {:keys [prefix chain] :or {prefix (createQueue) chain {}}}]
  "returns the next cleaned sentence"
  (if (.hasNext scanner)
    (let [rawWord (.next scanner) word (cleanWord rawWord)]
      (if (isParagraphBoundry rawWord)
        (recur scanner {:prefix (createQueue) :chain chain})
        (let [prefix (if (> (count prefix) 2) (pop prefix) prefix)
              chain (update-in chain [(build-prefix prefix) word] (fnil inc 0))]
          (recur scanner {:prefix (conj prefix word) :chain chain})
       )))
    chain))

(defn getNextWord [chain prefix]
  (let [states (seq (get chain (build-prefix prefix)))
        stateCount (reduce (fn [c [state count]] 
                  (+ c count)) 0 states)
        stateIndex (rand-int stateCount)]
      
      (loop [c 0 i 0]
        (let [state (nth states i)
              c (+ c (last state))]
          (if (>= c stateIndex)
            (first state)
            (recur c (inc i)))))))

(defn generateSentence [chain & {:keys [sentence prefix] :or {sentence (vector) prefix (createQueue)}}]
    (let [prefix (if (> (count prefix) 2) (pop prefix) prefix)
          word (getNextWord chain prefix)]
      (if (isLastWord word)
        (conj sentence word)
        (recur chain {:sentence (conj sentence word) :prefix (conj prefix word)}))))

(defn train [fileName chain]
  (let [sc (.useDelimiter (new Scanner (io/file fileName)) "([\t ]+)|([\n])") prefix (vector)]
    (let [chain (buildChain sc)]
      (println "chain size " (count chain))
      (println "need to walk chain to generate sentence")
      (println (generateSentence chain) )
      )))

(defn -main
  "I run the show"
  [& args]
  (let [chain (ref hash-map)]
    (train (nth args 0) chain)))

