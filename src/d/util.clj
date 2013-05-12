(ns d.util
  (:gen-class)
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [clojure.edn :as edn])
  (:import (org.apache.commons.codec.digest DigestUtils)
           (org.slf4j LoggerFactory)
           (java.security MessageDigest)))

(def pwd (-> "etc/pwd" slurp .trim))

(defn- _md5 [digest]
  (let [hexString (StringBuilder.)
        myRange (range (count digest))]
    (loop [i 0]
      (if (< i (count myRange))
        (let [h (Integer/toHexString (bit-and 255 (nth digest i)))]
          (if (< (nth digest i) 16)
            (.append (.append hexString "0") h)
            (.append hexString h))
          (recur (inc i)))
        (.toString hexString)))))

(defn md5 [s]
  (let [md (MessageDigest/getInstance "MD5")]
    (_md5 (.digest md (.getBytes s)))))
    
(defn- _gen-pwd [curmillis]
  (let [hash (md5 (str pwd "?" curmillis))]
    (format "%s,%s" curmillis hash)))

(defn gen-pwd []
  (_gen-pwd (Long/toString (System/currentTimeMillis))))

(defn seq-str [seq]
  (let [interposed (interpose " " seq)]
    (let [sb (StringBuilder.)]
      (loop [i 0]
        (if (< i (count interposed))
          (do
            (.append sb (nth interposed i))
            (recur (inc i)))
          (format "[%s]" (.toString sb)))))))

(def props (edn/read-string (slurp "etc/props.clj")))

(.info (LoggerFactory/getLogger "d.util") (str props))

(def ppc (Integer/valueOf (:pages-per-chunk props)))

(defn chunk-range [chunk]
  (range
   (* chunk ppc)
   (* (inc chunk) ppc)))

(defn- convert-row [row]
  (let [base 
        {:_id (:id row)
         :attrs (:attrs row)
         :pid (:productId row)
         :sid (:shopId row)
         :price (:price row)
         :retailer (:retailer row)
         :category (:category row)
         :location (format "%f,%f" (:lat row) (:lng row))
         :address (:address row)
         :shopname (:shopName row)
         :agroup (:agroup row)
         :options (:options row)}]
    (if (contains? row :web)
      (merge row 
             {:link (:link (:web row))
              :price (:price (:web row))
              :shipping (:shipping (:web row))})
      row)))

(defn extract-rows [o]
  (map convert-row (:result o)))
