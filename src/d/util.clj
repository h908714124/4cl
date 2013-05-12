(ns d.util
  (:gen-class)
  (:require [cheshire.core :as json]
            [clj-http.client :as client]
            [clojure.edn :as edn])
  (:import (org.apache.commons.codec.digest DigestUtils)
           (org.slf4j LoggerFactory)
           (java.security MessageDigest)))

(def props-file "etc/props.clj")
(def props (edn/read-string (slurp props-file)))

(defn- log-info [msg] (.info (LoggerFactory/getLogger "d.util") msg))

(log-info (str props-file ": " props))

(def ppc (Integer/valueOf (:pages-per-chunk props)))
(def pwd (:pwd props))

(defn- md5Hex [#^bytes bytes]
  (str "Formats a byte array as a string of lowercase hexadecimal "
       "characters. Unlike DigestUtil.md5Hex, this will format each "
       "byte as 2 lowercase hex digits. DigestUtils, as well "
       "as many other tools, may omit a leading zero if the byte "
       "value is lower than 16.")
  (let [sb (StringBuilder.)
        myRange (range (count bytes))]
    (loop [i 0]
      (if (< i (count myRange))
        (let [h (Integer/toHexString (bit-and 255 (nth bytes i)))]
          (if (< (nth bytes i) 16)
            (.append (.append sb "0") h)
            (.append sb h))
          (recur (inc i)))
        (.toString sb)))))

(defn md5 [s]
  (let [md (MessageDigest/getInstance "MD5")]
    (md5Hex (.digest md (.getBytes s)))))
    
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
      (merge base
             {:link (:link (:web row))
              :price (:price (:web row))
              :shipping (:shipping (:web row))})
      base)))

(defn extract-rows [o]
  (map convert-row (:result o)))
