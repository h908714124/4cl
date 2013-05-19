(ns d.db
  (:gen-class)
  (:require [clojure.java.jdbc :as j]
            [clojure.java.jdbc.sql :as s])
  (:use [clojure.tools.logging :only (infof errorf warnf debugf)])
  (:import (java.util Locale)))

(def jdbc-str-db
  "jdbc:mysql://root:@localhost:3306/foo")

(defn -main [& args]
  (Locale/setDefault (Locale/US))
  (let [conn (j/get-connection jdbc-str-db) 
        result (j/query conn ["select * from t"])]
  (infof "result: %s" (count result))))

  

