(ns d.job
  (:gen-class)
  (:require [cheshire.core :as json]
            [clojure.tools.cli :refer [cli]]
            [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [try+ throw+]]
            [clj-http.client :as client])
  (:import (clojure.lang ExceptionInfo)
           (org.slf4j LoggerFactory)
           (java.util.concurrent CountDownLatch
                                 LinkedBlockingQueue)))

(def flog (LoggerFactory/getLogger "log.to.file"))

(defn pullp [url n]
  (client/get (format url n)))

(defn -main [& args]
  (.info flog (.toString (json/parse-string (:body (pullp (first args) 1))))))
