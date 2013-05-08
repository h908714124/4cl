(defproject net.4cl/d
  (-> "etc/version.txt" slurp .trim)
  :description "curl -v https://sys.4chan.org/image/error/banned/250/rid.php"
  :url "4chan.org"
  :license {:name "none"
            :url "http://no.ne"}
  :resource-paths ["etc" "resources"]
  :dependencies [[cheshire "5.0.1"]
                 [clj-http "0.6.3"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.elasticsearch/elasticsearch-river-wikipedia
                  "1.2.0-SNAPSHOT"]
                 [org.twitter4j/twitter4j-stream "3.0.3"]
                 [slingshot "0.10.3"]]
  :plugins [[lein-bin "0.3.2"]]
  :main stream2es.main
  :bin {:bootclasspath true})
