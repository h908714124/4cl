(ns d.db
  (:gen-class)
  (:require [clojure.java.jdbc :as j]
            [clojure.tools.cli :refer [cli]]
            [cheshire.core :as json]
            [clojure.edn :as edn])
  (:use [clojure.java.io :only (writer)]))

(def db (edn/read-string (slurp "etc/db.clj")))

(def cli-opts
  [["-i" "--insert" :default "insert into t (c) values ('zoo %s tar')"]
   ["-n" "--num-rows" "Insert num rows" :default 10]
   ["-q" "--query" "Query" :default "select * from t limit 0, 2000000"]])

(defn- insert-test-data [db insert-sql num-rows]
  (j/db-transaction [db* db] 
                    (loop [counter (range num-rows)]
                      (when (seq counter)
                        (j/db-do-commands 
                         db false (format insert-sql (first counter)))
                        (recur (rest counter))))))
  
(defn -main [& args]
  (let [opts (first (apply cli args cli-opts))
        conn (j/get-connection "jdbc:mysql://root:@localhost:3306/foo")
        stmt (j/prepare-statement conn (:query opts) 
                                  :read-only true 
                                  :forward-only true
                                  :fetch-size (Integer/MIN_VALUE)
                                  :return-keys true)
        db {:connection conn}]
    (insert-test-data db (:insert opts) (Integer/valueOf (:num-rows opts)))
    (with-open [out (writer "log/s2es.txt")]
      (doseq [line (j/query db [stmt])]
        (.write out (str (json/generate-string line) "\n"))))))
  
