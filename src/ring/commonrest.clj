(ns ring.commonrest 
  (:use ring.util.response [clojure.string :only (trim lower-case) ])
  (:require [clj-json.core :as json]
    [clojure.contrib.logging :as logging]
    [clojure.contrib.io :as io]))

(defn json-response "Data is the http body, status is optional httpcode and content-type is ex. application/vnd.yoursee+json" 
  [data content-type & [status]]
  {:status (or status 200)
   :headers {"Content-Type" content-type 
             "ETag" (str (hash data))}
   :body (json/generate-string data)})

(defn get-json-response [data]
  (cond
    (nil? data) (json-response data 404)
    (empty? data) (json-response data 404)
    (not (nil? data)) (json-response data)))

(defn put-json-response [ifmatch etag okfunc]
  (cond
    (not (= ifmatch etag)) (json-response nil 409)
    (= ifmatch etag) (json-response (eval okfunc) 204)))

; ################ pre and post condition and request wrapper ##################
(defn- reqlog [msg & vals]
  (let [line (apply format msg vals)]
    (logging/info line)))


(defn- json-ex-response "returns a json response, if http-code is null, 400 is used." [exception httpcode]
  (-> (response (str {:exception (str exception)}))
    (status (if (nil? httpcode) (400) (Integer/parseInt (trim httpcode))))
    (content-type "application/vnd.yousee.kasia2.error+json")))

(defn- find-error-code "" [text]
  (re-find #"\s[0-9]{3}\s" (str text)))

; (def #^{:doc "Default httpcode for error handling via wrap-request-log-and-error-handling and chk "} *error-code* 500) 

(defn- handle-req [app req]
  (try (app req)
    (catch Exception e (do (logging/error "exp caught" e) (json-ex-response e 500)))
    (catch AssertionError e (do (logging/error "assertion caught" e) (json-ex-response e (find-error-code e))))))

(defn wrap-request-log-and-error-handling
  "Wrap an app such that exceptions thrown within the wrapped app are caught
  and a helpful json response is returned.
  if the exception is an AssertionError, the messages string is parsed for a http-code (ex: 400,405...) se macro (chk [httpcode func] ).
  Other exceptions returns a http-code of 500.
  Futhermore any caught exception is logged as an Error, and all request and responses is logged as Debug.
  "
  [app]
  (fn [{:keys [request-method uri json-params params] :as req}]
    (let [start (System/currentTimeMillis)
          resp (handle-req app req)
          finish (System/currentTimeMillis)
          total (- finish start)
          ]
      (reqlog "time (%dms)\n request-log %s %s - [json-parms:  %s ]- [params: %s] \n response-log %s " total request-method uri json-params params resp)
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

(defn is-empty? "returns a boolean, false if data = nil, {}, \"\" or \"null\" otherwise true " [data]
    (cond (empty? data) false  
        (= data "{}") false
        (= data "") false 
        (= data "null")  false
        (= data "nil")  false
        :else true))

(defn route-not-found-text "return a string with : No Service defined with the given path" [] 
  "No Service defined with the given path")

