(ns ring.commonrest 
  (:use ring.util.response [clojure.string :only (trim lower-case)] )
  (:require [clj-json.core :as json]
            [clojure.contrib.logging :as logging]
            [clojure.contrib.io :as io]
            [clojure.walk :as walk :only [postwalk]]))

(defn- nil-or-empty? [val]
  (or
    (not val)
    (and (string? val) (empty? val))
    (and (coll? val) (empty? val))))

(defn- filter-func
"Helper function for (rm-all-nil-values). This function removes all nil entries in a map"
  [data]
  (into {} (remove #(nil-or-empty? (second %)) data)))

(defn prune-tree
  "Walks through the supplied data structure and remove nil or empty entries from the map.
   Limitation: Due to the way postwalk (and prewalk) walks the tree, it is not possible to
   remove nil values from inside lists."
  [data]
  (walk/postwalk #(if (map? %)
	       (filter-func %)
	       %) data))

(defn json-response
  "Data is the http body, :status is optional httpcode, :etag is optional calculated etag value and content-type is ex. application/vnd.yoursee+json. :cache-control and :expires are optional" 
  [data content-type & {:as attrs}] 
  (let [data (prune-tree data)
        res {:status (or (:status attrs) 200)
             :headers {"Content-Type" content-type 
                       "ETag" (str (if (:etag attrs) (:etag attrs) (hash data)))}
             :body (json/generate-string data)}
        res2 (if (:cache-control attrs)
               (assoc-in res [:headers "Cache-Control"] (:cache-control attrs))
               res)
        res3 (if (:expires attrs)
               (assoc-in res2 [:headers "Expires"] (:expires attrs))
               res2)]
    res3))

(defmacro try-catch-resp [ & e]
  "try catch macro der returnerer java exp som json dokument og fejl 500"
  `(try ~@e
        (catch Exception a# (json-response { :error (str a#) } "application/vnd.yousee.kasia2.error+json" :status 500))))

; ################ pre and post condition and request wrapper ##################
(defn- reqlog [msg & vals]
  (let [line (apply format msg vals)]
    (logging/info line)))

(defn- reqlog-debug [msg & vals]
  (let [line (apply format msg vals)]
    (logging/debug line)))

(defn json-ex-response "returns a json response, if http-code is null, 400 is used." [exception httpcode]
  (-> (response (json/generate-string {:exception (str exception)}))
    (status (if (nil? httpcode) 400 (Integer/parseInt (trim httpcode))))
    (content-type "application/vnd.yousee.kasia2.error+json")))

(defn- find-error-code "" [text]
  (re-find #"\s[0-9]{3}\s" (str text)))

(defn- handle-req [app req]
  (try (app req)
    (catch Exception e (do (logging/error "exp caught" e) (json-ex-response e 500)))
    (catch AssertionError e (let 
                               [trace (into-array StackTraceElement (merge [] (first (.getStackTrace e ))))]
                              (.setStackTrace e trace)
                              (logging/warn "assertion caught" e) 
                              (json-ex-response e (find-error-code e))))))

(defn filter-comm [opts data]
      (apply str (filter #(not (nil? %)) (for [key opts] 
                      (let [value (str (key data))]
                         (if (not (empty?  value))
                           (cond 
                                (= key :body) (str "[" (name key) ": " 
                                                   (if 
                                                       (instance? java.io.InputStream (key data)) 
                                                        nil
                                                        value ) "]" )
                                :else (str "[" (name key) ": " value "]" ))))))))

(defn wrap-request-log-and-error-handling
  "Wrap an app such that exceptions thrown within the wrapped app are caught
  and a helpful json response is returned.
  if the exception is an AssertionError, the messages string is parsed for a http-code (ex: 400,405...) se macro (chk [httpcode func] ).
  Other exceptions returns a http-code of 500.
  Futhermore any caught exception is logged as an Error, and all request and responses is logged as Debug.
  optional keys for logging :  
 
  :server-port :server-name :remote-addr :uri :query-string :scheme
  :request-method :content-type :content-length :character-encoding
  :headers :body :params :json-param 
  
  "
  [app & opts]
  (fn [{:keys [request-method uri] :as req}]
    (let [start (System/currentTimeMillis)
          resp (handle-req app req)
          finish (System/currentTimeMillis)
          total (- finish start)
          dump-req (filter-comm opts req) 
          dump-resp (filter-comm opts resp)]
      (reqlog "time (%dms)\n Request: %s %s \n Response: %s" total (name request-method) uri (:status resp))
      (reqlog-debug "\nReq-dump:\n %s \n Resp-dump:\n %s"  dump-req dump-resp)
      resp)
    )
  )

(defn- convert-to-lowercase-prefix [data prefix]
  (loop [result {} the-rest data ]
      (if (empty? the-rest)
        result
        (let [c (first the-rest)]
         (recur (assoc result (str prefix (lower-case (first c))) (last c)) (rest  the-rest))))))

(defn wrap-promote-header "Takes the given headers and adds them to params, as lowercase and prefixed with header_ 
                          This makes routes with headers in functions arguments possible. 
                          Ex. (GET '/somepath/:id' [id headertag] ...) headertag could be :
                          content-type, accept, uri, host, user-agent, content-length, character-encoding etc.
                          IMPORTANT : This wrapper must be executed before wrap-json-params" 
  [handler]
  (fn [request]
        (let [request* (assoc request :params (merge (:params request) (convert-to-lowercase-prefix (:headers request) "header_") ))] 
         (handler request*))))

(defmacro chk "Used in :pre and :post condition for setting the right http-code for AssertionErrors, the func argument is evaluated. " [^Integer httpcode func & comment] `(do ~func))

(defn is-empty? "returns a boolean, false if data = nil, {}, [], \"\" or \"null\" otherwise true " [data]
    (cond (empty? data) false  
        (= data "{}") false
        (= data "") false 
        (= data "null")  false
        (= data "nil")  false
        (= data "[]")  false
        :else true))

(defn route-not-found-text "return a string with : No Service defined with the given path" [] 
  "No Service defined with the given path")




