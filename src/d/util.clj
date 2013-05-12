(ns d.util
  (:gen-class)
  (:require [cheshire.core :as json]
            [clj-http.client :as client])
  (:import (org.apache.commons.codec.digest DigestUtils)
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