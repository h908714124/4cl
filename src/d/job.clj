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
(defn- file-out [str] (.info flog str))

(def log (LoggerFactory/getLogger "d.job"))

(defn- int-prop [key] (Integer/valueOf (key util/props)))

(def page-url (:page-url util/props))
(def worker-pool-size (int-prop :worker-pool-size))
(def http-pool-size ( int-prop :http-pool-size))
(def socket-timeout (* 1000 (int-prop :socket-timeout-seconds)))
(def conn-timeout (* 1000 (int-prop :conn-timeout-seconds)))
(def header (:header util/props))

(defn- dump-to-file [response]
  (let[o (json/parse-string (:body response) true) 
       rows (util/convert-json o)]
    (loop [rr rows]
      (if (not (empty? rr))
        (do
          (file-out (json/generate-string (first rr)))
          (recur (rest rr)))))
    (count rows)))

(defn- retry-handler [ex count context]
  (let [again (< count 10)
        msg (format "%s count=%d again?%b" ex count again)]
    (.warn log msg)
    again))

(defn- do-page [n]
  #(let [endpoint (format page-url n)]
    (dump-to-file
     (client/get endpoint 
                 {:headers {header (util/gen-pwd)}}))))

(defn- iterate-chunks [] 
  (let [pool (Executors/newFixedThreadPool worker-pool-size)]
    (try
      (loop [rr (range)]
        (let [page-range (util/chunk-range (first rr))
              tasks (map #(do-page %) page-range)
              result (map #(.get %) (.invokeAll pool tasks))
              chunk-str (util/seq-str page-range)
              msg (str "pages done: " chunk-str)]
          (.info log msg)
          (if (> (apply + result) 0)
            (recur (rest rr)))))
      (finally (.shutdown pool)))))

(defn -main [& args]
  (do
    (Locale/setDefault (Locale/US))
    (client/with-connection-pool {:threads http-pool-size
                                  :socket-timeout socket-timeout
                                  :conn-timeout conn-timeout
                                  :retry-handler retry-handler}
      (iterate-chunks))))

