(ns d.job
  (:gen-class)
  (:require [cheshire.core :as json]
            [d.util :refer :all]
            [clj-http.client :as client])
  (:import (org.slf4j LoggerFactory)
           (java.util.concurrent Executors)))

(def flog (LoggerFactory/getLogger "log.to.file"))
(def log (LoggerFactory/getLogger "d.job"))

(defn flogg [s] (.info flog s))

(def ppc 10)

(def header (-> "etc/header" slurp .trim))

(defn retry-handler [ex count context]
  (let [again (< count 10)
        msg (format "%s count=%d again?%b" ex count again)]
    (.warn log msg)
    again))

(defn tee [s]
  (let [msg (.toString s)]
    (do
      (flogg msg)
      (if (> (count msg) 20) 1 0))))

(defn pullp [url]
  (client/get url {:headers {header (gen-pwd)}
                   :retry-handler retry-handler
                   :socket-timeout (* 1800 1000)
                   :conn-timeout (* 1800 1000)}))

(defn writep [url n]
  (fn [] 
    (let [endpoint (format url n)]
      (tee 
       (json/parse-string 
        (:body 
         (pullp endpoint)))))))

(defn chunk-range [chunk]
  (range
   (* chunk ppc)
   (* (inc chunk) ppc)))

(defn chunk-writep [url range]
  (map 
   #(writep url %) range))

(defn iterate-chunks [url] 
  (let [pool (Executors/newFixedThreadPool 4)]
    (loop [chunk 0]
      (let [range (chunk-range chunk)
            tasks (chunk-writep url range)
            result (map #(.get %) (.invokeAll pool tasks))]
        (.info log (format "finished chunk: %d (pages: %s)" chunk (seq-str range)))
        (if (> (apply + result) 0)
               (recur (inc chunk))
               nil)))))

(defn -main [& args]
  (iterate-chunks (first args)))

