(ns markov-text.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import  (java.util Scanner) ))

(defn cleanWord [word]
  (-> word 
    .toLowerCase
    .trim
    (.replaceAll "[,-0-9*&:;\"']+" "")))

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
  (if-not (contains? chain prefix)
    (assoc! chain prefix {word (atom 1)})
    (let [futureStates (get chain prefix)]
      (if-not (contains? futureStates word)
        (assoc! futureStates word (atom 1))
        (swap! (get futureStates word) inc))
        chain)))

(defn addToChain [chain state nextState] 
  (let [nextStateMap (if (contains? chain state) (get chain state) {nextState (atom 0)})]
    
    (assoc! chain nextState nextStateMap)
    ))

(defn buildChain [scanner & {:keys [prefix chain] :or {prefix (createQueue) chain (transient {})}}]
  "returns the next cleaned sentence"
  (if (.hasNext scanner)
    (let [rawWord (.next scanner) word (cleanWord rawWord)]
      (if (isParagraphBoundry rawWord)
        (recur scanner {:prefix (createQueue) :chain chain})
        (let [prefix (if (> (count prefix) 2) (pop prefix) prefix)
              chain (addToChain chain (str/join " " prefix) word)]
          (recur scanner {:prefix (conj prefix word) :chain chain})
       )))
    (persistent! chain)))

(defn getNextWord [chain prefix]
  
  (let [states (seq (deref (get chain (str/join prefix))))
        stateCount (reduce 
                      (fn [stateCount stateAndCount] 
                        (+ stateCount (deref (get stateAndCount 1)))) 0 states)
        stateIndex (rand-int stateCount)]
    
      (loop [c 0 i 0]
        (let [c (+ c  
            (deref (get (nth states i) 1)))]
          (if (>= c stateIndex)
            (get (nth states i) 0)
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
      (println chain)
      (comment "need to walk chain to generate sentence")
      (println (generateSentence chain))
      )))

(defn -main
  "I run the show"
  [& args]
  (let [chain (ref hash-map)]
    (train (nth args 0) chain)))

