(defproject ring-common "1.0.0-SNAPSHOT"
  :description "Common wrappers for ring"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [ring/ring-core "0.3.3" :exclusions [javax.servlet/servlet-api]]
                 [clj-json "0.3.1"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                                  javax.jms/jms
                                                  com.sun.jdmk/jmxtools
                                                  com.sun.jmx/jmxri]]]
  :dev-dependencies
   [[lein-clojars "0.6.0"]
    [com.stuartsierra/lazytest "1.1.2"]
    ]
  :repl-init-script "init.clj" ; init.cjl  is not under version control. se sample-init.clj
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"})

(ns leiningen.lazytest
  (:import [java.io File])
  (:use [leiningen.compile :only [eval-in-project]]))

  (defn lazytest
    ([project] (lazytest project ""))
    ([project re]
      (eval-in-project
        project
        `(do
          (let [files# (map
            #(. % getPath)
            (file-seq (new File "test")))]
            (require 'lazytest.report.nested)
            (require 'lazytest.runner.console)
            (doseq [x# files#]
              (if (re-find #"clj$" x#)
                (load-file x#))))
          (let [pat# (re-pattern (str "unit.*" ~re))]
            (doseq [tns# (filter
              #(re-find pat# (str %))
              (all-ns))]
              (lazytest.report.nested/report
	       (lazytest.runner.console/run-tests tns#))))
	  (let [pat# (re-pattern (str "component.*" ~re))]
            (doseq [tns# (filter
              #(re-find pat# (str %))
              (all-ns))]
              (lazytest.report.nested/report
	       (lazytest.runner.console/run-tests tns#))))))))
