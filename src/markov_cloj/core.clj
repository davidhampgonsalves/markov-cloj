(ns markov-cloj.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import  (java.util Scanner)
            (clojure.lang PersistentQueue)))

; Reemmber if you change the state length you'll need to rebuild your chain
(def state-length 2)

(defn clean-word [word]
  "strip unwanted characters from our string"
  (-> word
    .trim
    (.replaceAll "[\"]+" "")))

(defn conj-prefix [prefix word]
  (conj prefix (str/upper-case (str/trim word))))

(defn last-word? [word]
  "Check word to see it its the last one in a sentence"
  (-> word
    .trim
    (.matches ".+[!.?]$")))

(defn prefix-str [prefix]
  "Join prefix into a string"
  (str/upper-case (str/join " " prefix)))

(defn build-chain [scanner & {:keys [prefix chain] :or {prefix (PersistentQueue/EMPTY) chain {}}}]
  "Build a Markov chain based on the provided scanner."
  (if (.hasNext scanner)
    (let [word (clean-word (.next scanner))
          prefix-count (count prefix)
          prefix (if (> prefix-count state-length) (pop prefix) prefix)
          chain (if (>= prefix-count state-length)
                  (update-in chain [(prefix-str prefix) word] (fnil inc 0)) chain)]
        (recur scanner {:prefix (conj-prefix prefix word) :chain chain}))
    chain))

(defn merge-chains [chains]
  (apply merge-with #(merge-with + % %2) chains))

(defn calculate-next-state [chain prefix]
  "determine a random initial state based on the chain"
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
  "create sentence by walking the chain and applying weights"
  (if (nil? prefix)
   (let [prefix (initial-state chain)]
     (recur chain prefix {:sentence (into []
       (str/split (str/capitalize
       (str/join " " prefix)) #" "))}))
    (let [word (calculate-next-state chain prefix)]
      (if (last-word? word)
        (conj sentence word)
        (recur chain (conj-prefix (pop prefix) word) {:sentence (conj sentence word)})))))

(defn create-scanner [fileName]
  "create a scanner on the given file"
  (.useDelimiter (new Scanner (io/file fileName)) "([ \t\r\n\f]+)"))

(defn read-chain [file-name]
  (read-string (slurp file-name)))

(defn write-chain [chain file-name]
  (spit file-name (.toString chain)))

(defn is-edn? [file-name]
  (re-find #"(?i)\.edn$" file-name))

(defn get-chain [args]
  (if (is-edn? (first args))
    (read-chain (first args))
    (let [args (filter #(not (is-edn? %)) args)
          chains (pmap #(build-chain (create-scanner %)) args)]
      (merge-chains chains))))

(defn -main
  "build markov chain based on supplied input and generate text from them."
  [& args]

  (let [chain (get-chain args)]
    ; if an output file was passed write chain edn
    (if (and (not (is-edn? (first args))) (is-edn? (last args)))
      (let [output-filename (last args)]
        (write-chain chain output-filename)
        (println "chain created:" output-filename))
      (loop [prefix nil sentence []]
        (let [sentence (concat sentence (generate-sentence chain prefix))]
          (if (< (count sentence) 30)
            (recur (into (PersistentQueue/EMPTY) (take-last state-length sentence)) sentence)
            (println (str/join " " sentence))))))
   (shutdown-agents)))
