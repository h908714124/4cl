(ns d.util-test
  (:require [clojure.test :refer :all]
            [d.util :refer :all])
  (:import (org.apache.commons.codec.digest DigestUtils)))

(deftest util-test 
  (println (gen-pwd)))

