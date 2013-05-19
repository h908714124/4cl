(ns d.job
  (:gen-class)
  (:require [cheshire.core :as json]
            [d.util :as util]
            [clojure.edn :as edn]
            [clj-http.client :as client])
  (:use [clojure.string :only (join)]
        [clojure.tools.logging :only (infof errorf debugf)])
  (:import (org.slf4j LoggerFactory)
           (java.util.concurrent Executors)
           (java.util Locale)
           (org.apache.http NoHttpResponseException)))

(def file-out (LoggerFactory/getLogger "log.to.file"))
(defn- write [str] (.info file-out str))

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
  (let [parsed-json (json/parse-string response-body true) 
        docs (map #(util/convert-doc %) (:result parsed-json))
        doc-lines (map #(json/generate-string %) docs)]
    (write (apply str (map #(str % "\n") doc-lines)))
    (count docs)))

(defn- create-page-task [endpoint]
  #(let [session (clj-http.cookies/cookie-store)]
     (infof "Starting download: %s" endpoint)
     (loop [counter (range max-retries)]
       (let [response (client/get endpoint 
                                  {:headers {header (util/gen-pwd)}
                                   :cookie-store session
                                   :throw-exceptions false})
             status (:status response)
             body (:body response)]
         (if (= 200 status)
           (let [dumped (dump-to-file body)]
             (infof "Done: %s (%s docs)" endpoint dumped))
           (if (and (= 408 status) (seq counter)) ;should retry?
             (do (debugf "Retrying %s [%s]" endpoint (first counter))
                 (recur (rest counter))) ;try again
             (errorf "%s: %d, body: %s" endpoint status body))))))) ;give up

(defn- download [num-pages] 
  (let [pool (Executors/newFixedThreadPool worker-pool-size)]
    (let [endpoints (reverse 
                     (map #(format page-url %) 
                          (range start-page (+ start-page num-pages))))
          tasks (map #(create-page-task %) endpoints)]
      (.invokeAll pool tasks)
      (.shutdown pool))))

(defn- count-docs []
  (if num-docs
    num-docs
    (let [session (clj-http.cookies/cookie-store)]
      (infof "Starting count query at: %s" count-endpoint)
      (loop [counter (range max-retries)]
        (let [response (client/get count-endpoint 
                                   {:cookie-store session
                                    :throw-exceptions false})
              status (:status response)]
          (if (= 200 status)
            (let [json (json/parse-string (:body response) true)]
              (Long/valueOf (:info json)))
            (do (if (= 15 (mod (first counter) 16))
                  (infof "Retrying count query: %s" 
                         (first counter)))
                (if (and (= 408 status) (seq counter))
                  (recur (rest counter))
                  (do (errorf "Error: %s" status) 0)))))))))

(defn- calculate-num-pages []
  (let [identity+ (fnil identity 0)
        num-docs (identity+ (count-docs))]
    (infof "Docs: %s" num-docs)
    (inc (quot (dec num-docs) page-size))))

(defn -main [& args]
  (Locale/setDefault (Locale/US))
  (client/with-connection-pool {:threads http-pool-size
                                :timeout socket-timeout
                                :default-per-route http-pool-size}
    (let [num-pages (calculate-num-pages)]
      (infof "Max memory: %s MiB" 
           (quot 
            (.maxMemory (Runtime/getRuntime)) 
            (* 1024 1024)))
      (infof "Pages: %s" num-pages)
      (download num-pages)
      (infof "Done."))))

