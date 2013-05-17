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
  "format a finite seq for printing"
  (let [interposed (interpose " " seq)]
    (let [sb (StringBuilder.)]
      (loop [i 0]
        (if (< i (count interposed))
          (do
            (.append sb (nth interposed i))
            (recur (inc i)))
          (format "[%s]" (.toString sb)))))))

(defn convert-doc [d]
  (let [local {:_id (:id d)
               :attrs (:attrs d)
               :pid (:productId d)
               :sid (:shopId d)
               :price (:price d)
               :retailer (:retailer d)
               :category (:category d)
               :location (format "%f,%f" (:lat d) (:lng d))
               :address (:address d)
               :shopname (:shopName d)
               :agroup (:agroup d)
               :options (:options d)}
        web (if (contains? d :web) 
              {:weblink (:link (:web d))
               :webprice (:price (:web d))
               :shipping (:shipping (:web d))})]
    (merge local web)))


