(ns ring.json-params
  (:require [clj-json.core :as json]
            [ring.commonrest :as cr :only (json-ex-response)]
            [clojure.contrib.logging :as logging]))

(defn- json-request?
  [header]
  (if header
     (not (empty? (re-find #"^application/(vnd.+)?json" header)))))

(defn wrap-json-params [handler]
  (fn [req]
    (if-let [body (and (or (json-request? ((:headers req) "content-type")) 
                           (json-request? ((:headers req) "accept"))) (:body req))]
      (try 
         (let [bstr (if (instance? java.io.InputStream body) (slurp body) body)
            json-params (json/parse-string bstr)
            req* (assoc req
                   :json-params json-params
                   :params (merge (:params req) json-params))]
           (handler req*))
       (catch org.codehaus.jackson.JsonParseException e 
         (logging/warn "Parsing json" e)
         (cr/json-ex-response e 400)))
      (handler req))))
