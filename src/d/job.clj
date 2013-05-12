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

(defn- int-prop [key] (Integer/valueOf (key util/props)))

(def page-url (:page-url util/props))
(def worker-pool-size (int-prop :worker-pool-size))
(def http-pool-size ( int-prop :http-pool-size))
(def socket-timeout (* 1000 (int-prop :socket-timeout-seconds)))
(def conn-timeout (* 1000 (int-prop :conn-timeout-seconds)))
(def header (:header util/props))

(defn- dump-to-file [s] 
  (let [rows (util/extract-rows s)]
    (if (= 0 (count rows))
      0
      (loop [row 0]
        (.info flog (json/generate-string (nth rows row)))
        (if (< row (dec (count rows)))
          (recur (inc row))
          (count rows))))))

(defn- retry-handler [ex count context]
  (let [again (< count 10)
        msg (format "%s count=%d again?%b" ex count again)]
    (.warn log msg)
    again))

(defn- writep [n]
  #(let [endpoint (format page-url n)]
     (dump-to-file 
      (json/parse-string 
       (:body 
        (client/get endpoint {:headers {header (util/gen-pwd)}})) true))))

(defn- iterate-chunks [] 
  (let [pool (Executors/newFixedThreadPool worker-pool-size)]
    (try
      (loop [chunk 0]
        (let [range (util/chunk-range chunk)
              tasks (map #(writep %) range)
              result (map #(.get %) (.invokeAll pool tasks))
              chunk-str (util/seq-str range)
              msg (format "finished chunk: %d (pages: %s)" chunk chunk-str)]
          (.info log msg)
          (if (> (apply + result) 0)
            (recur (inc chunk))
            nil)))
      (finally (.shutdown pool)))))

(defn -main [& args]
  (do
    (Locale/setDefault (Locale/US))
    (client/with-connection-pool {:threads http-pool-size
                                  :socket-timeout socket-timeout
                                  :conn-timeout conn-timeout
                                  :retry-handler retry-handler}
      (iterate-chunks))))

