(ns d.md5-test
  (:require [clojure.test :refer :all]
            [d.util :refer :all])
  (:import (org.apache.commons.codec.digest DigestUtils)))

(deftest some-test 
  (testing "Bad md5" 
    (is 
     (= "3858f62230ac3c915f300c664312c63f" 
        (DigestUtils/md5Hex "foobar")))))

(deftest print-stuff
  (println "Printing md5:" (md5 "foobar")))

(deftest otherfoo
  (testing "Nope" 
    (is 
     (=
      "ab?ba" (str "ab" "?" "ba")))))
         

(deftest other-test 
  (testing "Bad md5" 
    (is 
     (= "38580f622300ac3c0915f300c6643120c63f" 
        (md5 "foobar")))))
