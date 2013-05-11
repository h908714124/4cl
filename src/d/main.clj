(ns d.main
  (:gen-class)
  (:require [cheshire.core :as json]
            [clojure.tools.cli :refer [cli]]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [try+ throw+]])
  (:import (clojure.lang ExceptionInfo)
           (java.util.concurrent CountDownLatch
                                 LinkedBlockingQueue)))

(defn hotpo [^Integer arg] 
  (if (even? arg) 
    (/ arg 2) 
    (+ 1 (* arg 3))))

(defn print-until-1 [some-seq] (loop [s some-seq]
   (do (print (first s) " ")
       (if (not= (first s) 1)
         (recur (rest s))
         nil))))

(defn -main [& args] 
  (do (print-until-1 (iterate hotpo (Integer/valueOf (first args))))
      (println)))

