(ns d.job
  (:gen-class)
  (:require [cheshire.core :as json]
            [d.util :refer :all]
            [clj-http.client :as client])
  (:import (org.slf4j LoggerFactory)
           (java.util.concurrent Executors)))

(def flog (LoggerFactory/getLogger "log.to.file"))

(defn flogg [s] (.info flog s))

(def header (-> "etc/header" slurp .trim))

(defn tee [s]
  (let [msg (.toString s)]
    (do
      (flogg msg)
      msg)))

(defn pullp [url]
  (client/get url {:headers {header (gen-pwd)}}))

(defn writep [url n]
  (fn [] 
    (tee 
     (json/parse-string 
      (:body 
       (pullp 
        (format url n)))))))

(defn iterate-pages [url] 
(let [pool (Executors/newFixedThreadPool 4)]
  (loop [p 0]
    (let [task (writep url p) 
          result (task)]
      (if (> (count result) 20)
        (recur (inc p))
        nil)))))

(defn -main [& args]
   (iterate-pages (first args)))

