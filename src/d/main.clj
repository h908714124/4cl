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

(defn -main [& args] (println 
  (let [hotpo-seq (iterate hotpo 27)]
    (take 
     (-> (.indexOf hotpo-seq 1) inc) hotpo-seq))))
