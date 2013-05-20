(ns d.db
  (:gen-class)
  (:require [clojure.java.jdbc :as j]
            [clojure.edn :as edn]))

(def db (edn/read-string (slurp "etc/db.clj")))

(defn -main [& args]
  (j/query db ["select * from t"]))
