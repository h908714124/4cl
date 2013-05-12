(ns d.job
  (:gen-class)
  (:require [cheshire.core :as json]
            [d.util :as util]
            [clojure.edn :as edn]
            [clj-http.client :as client])
  (:import (org.slf4j LoggerFactory)
           (java.util.concurrent Executors)
           (java.util Locale)))

(def flog (LoggerFactory/getLogger "log.to.file"))
(def log (LoggerFactory/getLogger "d.job"))

(def worker-pool-size (Integer/valueOf (:worker-pool-size util/props)))
(def http-pool-size (Integer/valueOf (:http-pool-size util/props)))
(def socket-timeout (* 1000 (Integer/valueOf (:socket-timeout-seconds util/props))))
(def conn-timeout (* 1000 (Integer/valueOf (:conn-timeout-seconds util/props))))

(defn- dump-to-file [s] 
  (let [rows (util/extract-rows s)]
    (if (= 0 (count rows))
      0
      (loop [row 0]
        (.info flog (json/generate-string (nth rows row)))
        (if (< row (dec (count rows)))
          (recur (inc row))
          (count rows))))))

(def header (-> "etc/header" slurp .trim))

(defn- retry-handler [ex count context]
  (let [again (< count 10)
        msg (format "%s count=%d again?%b" ex count again)]
    (.warn log msg)
    again))

(defn- pullp [url]
  (client/get url {:headers {header (util/gen-pwd)}
                   :retry-handler retry-handler
                   :socket-timeout socket-timeout
                   :conn-timeout conn-timeout}))

(defn- writep [url n]
  #(let [endpoint (format url n)]
     (dump-to-file 
      (json/parse-string 
       (:body 
        (pullp endpoint)) true))))


(defn- chunk-writep [url range]
  (map 
   #(writep url %) range))

(defn- iterate-chunks [url] 
  (let [pool (Executors/newFixedThreadPool worker-pool-size)]
    (try
      (loop [chunk 0]
        (let [range (util/chunk-range chunk)
              tasks (chunk-writep url range)
              result (map #(.get %) (.invokeAll pool tasks))]
          (.info log (format "finished chunk: %d (pages: %s)" chunk (util/seq-str range)))
          (if (> (apply + result) 0)
            (recur (inc chunk))
            nil)))
      (finally (.shutdown pool)))))

(defn -main [& args]
  (do
    (Locale/setDefault (Locale/US))
    (client/with-connection-pool {:threads http-pool-size}
      (iterate-chunks (first args)))))

