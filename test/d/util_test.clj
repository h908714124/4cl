(ns d.util-test
  (:require [clojure.test :refer :all]
            [d.util :refer :all]
            [d.main :as hotpo])
  (:import (org.apache.commons.codec.digest DigestUtils)))

(deftest util-test 
  (println (gen-pwd)))

(deftest seq-str-test 
  (testing "Bad result" 
    (is
     (= "[0 1 2]" (seq-str (range 0 3))))))

(deftest nil-test 
  (testing "loop returns nil" 
    (is
     (= nil
        (loop [i 0]
          (if (< i 0)
            (recur (inc i))))))))

(deftest nil-map
  (testing "nil map"
    (is (= nil nil))))

(deftest anon-map
  (testing "will it work"
    (let [mapped (map
                  #(let [n %]
                     (hotpo/hotpo n))
                  '(27 38))]
      (is 
       (= 101
          (apply + mapped))))))

(deftest must-throw
  (let [deref-inc
        (fn [a]
          (inc (deref a)))]
    (testing "must throw"
      (is 
       (thrown? NullPointerException
                (deref-inc nil))))
    (testing "catch-return"
      (is
       (= 1
          (try
            (deref-inc nil)
            (catch NullPointerException e 
              (println "caught " (.getMessage e)) 
              1)))))))

(deftest id+-test
  (testing "will it work"
    (let [identity+ (fnil identity 0)]
      (is (nil? (identity nil)))
      (is (zero? (identity+ nil))))))

(defmacro id-macro [f] f)
(defmacro eval-macro [f] `(~f))
(deftest macro-test
  (testing "will it work"
    (is (= 1 (eval-macro (fn [] 1))))
    (let [result (id-macro (fn [] 1))]
      (is (= 1 (eval (result)))))))
