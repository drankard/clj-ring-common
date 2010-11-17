(ns common.ring.commonrest (:use ring.util.response)
  (:require [clj-json.core :as json]
    [clojure.contrib.logging :as logging]
    [clojure.contrib.io :as io]))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/vnd.yousee+json"
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
    (status (if (nil? httpcode) (400) (Integer/parseInt (str httpcode))))
    (content-type "application/vnd.yousee+json")))

(defn- handle-req [app req]
  (try (app req)
    (catch Exception e (do (logging/error "exp caught" e) (json-ex-response e 500)))
    (catch AssertionError e (do (logging/error "assertion caught" e) (json-ex-response e (re-find #"[0-9]{3}" (str e)))))))

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
(defn wrap-promote-header "Takes the given headers and adds them to params; This makes routes with headers in functions arguments possible. 
                          Ex. (GET '/somepath/:id' [id headertag] ...) headertag could be content-type, accept, uri, host, user-agent, content-length, character-encoding etc. "  [handler]
  (fn [request]
        (let [request* (assoc request :params (merge (:params request) (:headers request) ))] 
         (handler request*))))

(defmacro chk "Used in :pre and :post condition for setting the right http-code for AssertionErrors, the func argument is evaluated. " [httpcode func & comment] `(do ~func))

(defn is-empty? "returns a boolean, false if data = nil, {}, \"\" or \"null\" otherwise true " [data]
    (cond (empty? data) false  
        (= data "{}") false
        (= data "") false 
        (= data "null")  false
        :else true))
