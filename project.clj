(defproject ring-common "1.0.3"
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
    [lein-lazytest "1.0.1"]
    [com.stuartsierra/lazytest "1.1.2"]
    ]
  :lazytest-path ["src" "test"]            
  :repl-init-script "init.clj" ; init.cjl  is not under version control. se sample-init.clj
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"})

