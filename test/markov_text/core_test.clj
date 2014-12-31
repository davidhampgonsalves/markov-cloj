(ns markov-cloj.core-test
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
    (let [chain {"END. THE" {"START" 1}
                 "THE START" {"HAS" 1}
                 "START HAS" {"ENDED." 1}}]
      (is (= (generate-sentence chain nil)
             ["The" "start" "HAS" "ENDED."])))))

(deftest build-chain-test
  (testing "builds chain"
    (let [scanner (create-scanner "input/test.txt")
          chain (build-chain scanner)]
      (is (= chain 
             {"A SIMPLE" {"sentence." 1}, "OF A" {"simple" 1}, "TEST, OF" {"a" 1} 
              "A TEST," {"of" 1}, "IS A" {"test," 1}, "THIS IS" {"a" 1}}))))
  
  (testing "build chain from multiple sources"
    (let [chain1 (build-chain (create-scanner "input/test.txt"))
          chain2 (build-chain (create-scanner "input/test.txt"))
          chains [chain1 chain2]]
      (is (= (merge-chains chains) 
             {"A SIMPLE" {"sentence." 2}, "OF A" {"simple" 2}, 
             "TEST, OF" {"a" 2}, "A TEST," {"of" 2}, "IS A" {"test," 2}, 
             "THIS IS" {"a" 2}})))))
