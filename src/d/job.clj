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

(defn- int-prop [k] (let [i (util/prop k)] (if i (Integer/valueOf i))))

(def num-docs (int-prop :num-docs))
(def start-page (let [i (int-prop :start-page)] (if i i 0)))
(def page-url (util/prop :page-url))
(def count-endpoint (util/prop :count-endpoint))
(def page-size (int-prop :page-size))
(def worker-pool-size (int-prop :worker-pool-size))
(def http-pool-size ( int-prop :http-pool-size))
(def socket-timeout (* 1000 (int-prop :socket-timeout-seconds)))
(def conn-timeout (* 1000 (int-prop :conn-timeout-seconds)))
(def header (util/prop :header))
(def max-retries (util/prop :retries))

(defn- dump-to-file [response-body]
  "returns number of docs written"
  (let[parsed-json (json/parse-string response-body true) 
       docs (map #(util/convert-doc %) (:result parsed-json))
       docs-str (map #(json/generate-string %) docs)]
    (file-write-line (join "\n" docs-str))
    (count docs)))

(defn- create-page-task [endpoint]
  #(let [session (clj-http.cookies/cookie-store)]
     (log "starting download: %s" endpoint)
     (loop [counter (range max-retries)]
       (let [response (client/get endpoint 
                                  {:headers {header (util/gen-pwd)}
                                   :cookie-store session
                                   :throw-exceptions false})
             status (:status response)]
         (if (= 200 status)
           (let [dumped (dump-to-file (:body response))]
             (log "finished download: %s (%s docs)" endpoint))
           (if (and (= 408 status) counter) ;should retry?
             (recur (rest counter)) ;try again
             (log "%s: %d" endpoint status))))))) ;give up

(defn- download [num-pages] 
  (let [pool (Executors/newFixedThreadPool worker-pool-size)]
    (let [endpoints (map #(format page-url %) 
                         (range start-page (+ start-page num-pages)))
          tasks (map #(create-page-task %) endpoints)]
      (.invokeAll pool tasks)
      (.shutdown pool))))

(defn- count-docs []
  (if num-docs
    num-docs
    (let [session (clj-http.cookies/cookie-store)]
      (log "starting count query at %s" count-endpoint)
      (loop []
        (let [response (client/get count-endpoint 
                                   {:cookie-store session
                                    :throw-exceptions false})
              status (:status response)]
          (if (= 200 status)
            (let [json (json/parse-string (:body response) true)]
              (Long/valueOf (:info json)))
            (if (= 408 status)
              (do (log "retrying count query") (recur))
              (do (log "error: %s" status) 0))))))))
  
(defn- calculate-num-pages []
  (let [num-docs (count-docs)]
    (log "Docs: %s" num-docs)
    (inc (quot (dec num-docs) page-size))))

(defn -main [& args]
  (Locale/setDefault (Locale/US))
  (client/with-connection-pool {:threads http-pool-size
                                :timeout socket-timeout
                                :default-per-route http-pool-size}
    (let [num-pages (calculate-num-pages)]
      (log "Max memory: %s MiB" 
           (quot 
            (.maxMemory (Runtime/getRuntime)) 
            (* 1024 1024)))
      (log "Pages: %s" num-pages)
      (download num-pages)
      (log "Done"))))

