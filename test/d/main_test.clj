(ns d.main-test
  (:require [clojure.test :refer :all]
            [d.main :refer :all]))

(deftest some-test 
  (testing "Should have tripled." 
    (is (= 10 (hotpo 3)))))
