(ns markov-text.core-test
  (:require [clojure.test :refer :all]
            [markov-text.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(deftest last-word-check 
  (testing "with spaces"
    (is (isLastWord " asdf. ")))
  (testing "with ?" 
    (is (isLastWord "asdf?"))))
