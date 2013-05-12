(ns d.util-test
  (:require [clojure.test :refer :all]
            [d.util :refer :all])
  (:import (org.apache.commons.codec.digest DigestUtils)))

(deftest util-test 
  (println (gen-pwd)))

(deftest seq-str-test 
  (testing "Bad result" 
    (is
     (= "[0 1 2]" (seq-str (range 0 3))))))



