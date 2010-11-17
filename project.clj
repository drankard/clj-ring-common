(defproject kunde "1.0.0-SNAPSHOT"
  :description "REST API til at finde kunder og kundedata, samt oprette/opdatere disse"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [compojure "0.5.1"]
                 [ring/ring-core "0.3.3" :exclusions [javax.servlet/servlet-api]]
                 [ring/ring-servlet "0.3.3" :exclusions [javax.servlet/servlet-api]]
                 [ring-json-params "0.1.1"]
                 [oracle/ojdbc "1.4"]
                 [clj-json "0.3.1"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                                  javax.jms/jms
                                                  com.sun.jdmk/jmxtools
                                                  com.sun.jmx/jmxri]]]
  :dev-dependencies
  [[uk.org.alienscience/leiningen-war "0.0.9"]
   [swank-clojure "1.2.1"]
   [ring/ring-devel "0.3.3"]
   [ring/ring-jetty-adapter "0.3.3"]
   [autodoc "0.7.1" :exclusions [enlive/enlive org.clojure/clojure-contrib]]
  
   [yousee/enlive "1.0.0"]
   [org.ccil.cowan.tagsoup/tagsoup "1.2"]
   [com.stuartsierra/lazytest "1.1.2"]
   [javax.servlet/servlet-api "2.5"]]
  :namespaces [kunde.servlet]
  :repl-init-script "init.clj" ; init.cjl  is not under version control. se sample-init.clj
  :web-content "public"
  :autodoc {
                      :name "Kasia 2.0"
                      :description "YouSee's Kunde system"
                      :copyright "Copyright 2010 YouSee a/s"
                      :output-path "public/autodoc"}
  )


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
