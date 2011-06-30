(ns ring.json-params
  (:require [clj-json.core :as json]))

(defn- json-request?
  [header]
  (if header
     (not (empty? (re-find #"^application/(vnd.+)?json" header)))))

(defn wrap-json-params [handler]
  (fn [req]
    (if-let [body (and (or (json-request? ((:headers req) "content-type")) (json-request? ((:headers req) "accept"))) (:body req))]
      (let [bstr (slurp body)
            json-params (json/parse-string bstr)
            req* (assoc req
                   :json-params json-params
                   :params (merge (:params req) json-params))]
        (handler req*))
      (handler req))))