(defproject net.4cl/d
  (.trim 
   (let [f (java.io.File. "etc/v")]
     (if (and (.exists f) (.isFile f))
       (slurp f)
       (let [ts-version (str "unspecified-" (System/currentTimeMillis))]
         (println (str 
                   "Version file etc/v not found. "
                   "Using timestamped version: " 
                   ts-version))
         ts-version))))
  :description "curl -v https://sys.4chan.org/image/error/banned/250/rid.php"
  :url "4chan.org"       
  :license {:name "none"
            :url "http://no.ne"}
  :resource-paths ["etc" "resources"]
  :dependencies [[cheshire "5.1.1"]
                 [org.clojure/java.jdbc "0.3.0-SNAPSHOT"]
                 [mysql/mysql-connector-java "5.1.25"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [clj-http "0.7.1"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [slingshot "0.10.3"]
                 [commons-codec/commons-codec "1.7"]
                 [ch.qos.logback/logback-classic "1.0.11"]
                 [org.codehaus.groovy/groovy-all "2.1.3"]]
  :source-paths ["src"]
  :plugins [[lein-bin "0.3.2"]]
  :main d.db
  :jvm-opts ["-Xms256m" "-Xmx2048m"]
  :bin {:bootclasspath true})
