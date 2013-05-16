(ns d.job
  (:gen-class)
  (:require [cheshire.core :as json]
            [d.util :as util]
            [clojure.edn :as edn]
            [clj-http.client :as client])
  (:use [clojure.string :only (join)])
  (:import (org.slf4j LoggerFactory)
           (java.util.concurrent Executors)
           (java.util Locale)
           (org.apache.http NoHttpResponseException)))

(def flog (LoggerFactory/getLogger "log.to.file"))
(defn- file-println [str] (.info flog str))

(def logger (LoggerFactory/getLogger "d.job"))
(defn- log 
  ([] (.info logger "logging with no args"))
  ([msg] (.info logger msg))
  ([f & msg] (.info logger (apply format f msg))))

(defn- int-prop [key] (Integer/valueOf (util/prop key)))

(def page-url (util/prop :page-url))
(def worker-pool-size (int-prop :worker-pool-size))
(def http-pool-size ( int-prop :http-pool-size))
(def socket-timeout (* 1000 (int-prop :socket-timeout-seconds)))
(def conn-timeout (* 1000 (int-prop :conn-timeout-seconds)))
(def header (util/prop :header))
(def retries (util/prop :retries))

(defn- dump-to-file [response]
  (let[o (json/parse-string (:body response) true) 
       rows (util/convert-json o)
       string-rows (map #(json/generate-string %) rows)]
    (file-println (join "\n" string-rows))
    (count rows) ;zero is stop signal
    ))

(defn- retry-handler [ex count context]
  (let [again (< count retries)
        msg (format "%s count=%d again?%b" ex count again)]
    (log msg)
    again))

(defn- do-page [n]
  #(let [endpoint (format page-url n)]
     (try
       (dump-to-file
        (client/get endpoint 
                    {:headers {header (util/gen-pwd)}}))
       (catch Exception e 
         (log "caught exception: %s" (.getMessage e))
         0 ;better use a stop signal here
         ))))

(defn- iterate-chunks [] 
  (let [pool (Executors/newFixedThreadPool worker-pool-size)]
    (try
      (loop [rr (range)] ;infinite range
        (let [page-range (util/chunk-range (first rr))
              tasks (map #(do-page %) page-range)
              result (map #(.get %) (.invokeAll pool tasks))
              chunk-str (util/seq-str page-range)
              msg (str "pages done: " chunk-str)
              result-size (apply + result)]
          (log "%s, result-size: %s" msg result-size)
          (if (not (zero? result-size)) ;stop signal
            (recur (rest rr)))))
      (finally (.shutdown pool)))))

(defn -main [& args]
  (do
    (Locale/setDefault (Locale/US))
    (log "Max memory: %f MiB" 
         (float (/ 
                 (.maxMemory (Runtime/getRuntime)) 
                 (* 1024 1024))))
    (client/with-connection-pool {:threads http-pool-size
                                  :socket-timeout socket-timeout
                                  :conn-timeout conn-timeout
                                  :retry-handler retry-handler}
      (iterate-chunks))
    (log "finish")))

