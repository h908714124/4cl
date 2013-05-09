(ns d.main
  (:gen-class)
  ;; Need to require these because of the multimethod in s.stream.
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
  (take 10 
    (iterate hotpo 27))))
