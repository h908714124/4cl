(ns d.job
  (:gen-class)
  (:require [cheshire.core :as json]
            [clojure.tools.cli :refer [cli]]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [try+ throw+]]
            [clj-http.client :as client])
  (:import (clojure.lang ExceptionInfo)
           (java.util.concurrent CountDownLatch
                                 LinkedBlockingQueue)))

(defn pullp [url n]
  (client/get (format url n)))

(defn -main [& args]
  println (json/parse-string (:body (pullp (first args) 1))))
