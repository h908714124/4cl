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
(defn prop [key]
  (let [value (key props)]
    (log-info (str "key: " key ", value: " value))
    value))

(def ppc (Integer/valueOf (:pages-per-chunk props)))
(def pwd (:pwd props))

(defn- md5Hex [bytes]
  "Formats the  byte seq input as a string of lowercase hexadecimal
   digits. Each byte is turned into a pair of lowercase hex digits, 
   regardless if its value is lower than 16."
  (let [sb (StringBuilder.)]
    (loop [bb bytes]
      (if (not (empty? bb))
        (let [head (first bb)
              h (Integer/toHexString (bit-and 255 head))]
          (if (< head 16)
            (.append (.append sb "0") h)
            (.append sb h))
          (recur (rest bb)))
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

(defn page-range [chunk]
  (range
   (* chunk ppc)
   (* (inc chunk) ppc)))

(defn convert-json [o]
  (map 
   #(let [row %
          loc {:_id (:id row)
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
               :options (:options row)}
          web (if (contains? row :web) 
                {:weblink (:link (:web row))
                 :webprice (:price (:web row))
                 :shipping (:shipping (:web row))}
                {})]
      (merge loc web))
   (:result o)))

