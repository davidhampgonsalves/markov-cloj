(ns markov-text.core-test
  (:require [clojure.test :refer :all]
            [markov-text.core :refer :all]
            [clojure.string :as str])
  (:import  (clojure.lang PersistentQueue)))

(deftest last-word-check 
  (testing "with spaces"
    (is (last-word? " asdf. ")))
  (testing "with ?" 
    (is (last-word? "asdf?"))))

(deftest initial-state-test
  (testing "orders state corectly"
    (is (=
      (first (initial-state {"THIS. IS A" {"test" 1}})) 
      "IS")))
  (testing "picks the right one"
    (is (= 
      (last (initial-state {"ONE TWO. THREE" {"FOUR" 1} "TWO. THREE FOUR" {"FIVE" 1}}))
      "FIVE"))))

(deftest generate-sentence-test 
  (testing "generates sentence"
    (let [chain {"THE START" {"HAS" 1}
                 "START HAS" {"ENDED." 1}}]
      (is (= (generate-sentence chain (into PersistentQueue/EMPTY ["THE" "START"]))
             ["THE" "START" "HAS" "ENDED."])))))
