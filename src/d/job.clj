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

(def ppc 40)

(def header (-> "etc/header" slurp .trim))

(defn tee [s]
  (let [msg (.toString s)]
    (do
      (flogg msg)
      (if (> (count msg) 20) 1 0))))

(defn pullp [url]
  (client/get url {:headers {header (gen-pwd)}}))

(defn writep [url n]
  (fn [] 
    (let [endpoint (format url n)]
      (.info log endpoint)
      (tee 
       (json/parse-string 
        (:body 
         (pullp endpoint)))))))

(defn chunk-writep [url chunk]
  (map 
   #(writep url %) 
   (range 
    (* chunk ppc) 
    (* (inc chunk) ppc))))

(defn iterate-chunks [url] 
  (let [pool (Executors/newFixedThreadPool 4)]
    (loop [chunk 0]
      (let [tasks (chunk-writep url chunk)
            result (map #(.get %) (.invokeAll pool tasks))]
        (if (> (apply + result) 0)
               (recur (inc chunk))
               nil)))))

(defn -main [& args]
  (iterate-chunks (first args)))

