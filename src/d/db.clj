(ns d.db
  (:gen-class)
  (:require [clojure.java.jdbc :as j]
            [clojure.edn :as edn]))

(def db (edn/read-string (slurp "etc/db.clj")))

(defn -main [& args]
  (let [conn (j/get-connection "jdbc:mysql://root:@localhost:3306/foo")
        stmt (j/prepare-statement conn "select * from t" 
                                  :read-only true 
                                  :forward-only true
                                  :fetch-size (Integer/MIN_VALUE)
                                  :return-keys true)
        result (j/query conn [stmt])]
    (println result)))
