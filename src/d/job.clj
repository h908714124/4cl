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
(defn- file-write-line [str] (.info flog str))

(def logger (LoggerFactory/getLogger "d.job"))

(defn- log 
  "returns nil"
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
  "returns number of docs written"
  (let[root (json/parse-string (:body response) true) 
       docs (map #(util/convert-json %) (:result root))
       sdocs (map #(json/generate-string %) docs)]
    (file-write-line (join "\n" sdocs))
    (count docs)))

(defn- retry-handler [ex count context]
  (let [again (< count retries)
        msg (format "%s count=%d again?%b" ex count again)]
    (log msg)
    again))

(defn- get-page [endpoint cs]
  (try
    (client/get endpoint 
                {:headers {header (util/gen-pwd)}
                 :retry-handler retry-handler
                 :cookie-store cs})
    (catch Exception e 
      (log "ex: %s" (.getMessage e)))))

(defn- page-task [p]
  #(let [session (clj-http.cookies/cookie-store)
         endpoint (format page-url p)]
     (loop [counter (range retries)]
       (let [page-data (get-page endpoint session)]
         (if (not (nil? page-data))
           (dump-to-file page-data)
           (if (counter)
             (recur (rest counter))
             0))))))

(defn- iterate-chunks [] 
  (let [pool (Executors/newFixedThreadPool worker-pool-size)]
    (loop [chunk (range)]
      (let [pages (util/page-range (first chunk))
            tasks (map #(page-task %) pages)
            results (map #(.get %) (.invokeAll pool tasks))]
        (log "chunk: %s docs: %s" 
             (first chunk) 
             (apply + results))
        (if (not (zero? (apply + results))) ;stop signal
          (recur (rest chunk)))))
    (.shutdown pool)))

(defn -main [& args]
  (do
    (Locale/setDefault (Locale/US))
    (log "Max memory: %f MiB" 
         (float (/ 
                 (.maxMemory (Runtime/getRuntime)) 
                 (* 1024 1024))))
    (client/with-connection-pool {:threads http-pool-size
                                  :socket-timeout socket-timeout
                                  :conn-timeout conn-timeout}
      (iterate-chunks))
    (log "finish")))

