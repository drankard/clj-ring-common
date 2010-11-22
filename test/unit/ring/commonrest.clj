(ns unit.ring.commonrest 
  (:require [ring.commonrest :as c])
  (:use
          [lazytest.describe :only (describe before after it given do-it for-any with)])
  (:import java.io.ByteArrayInputStream))

(defn- stream [s]
    (ByteArrayInputStream. (.getBytes s "UTF-8")))

(def json-echo
    (c/wrap-promote-header identity))

(def headers {"content-length" "15", "Etags" "sdfsdffa", "content-type" "application/json", "accept" "*/*", "host" "localhost:8080", "User-Agent" "curl/7.19.7 (i486-pc-linux-gnu) libcurl/7.19.7 OpenSSL/0.9.8k zlib/1.2.3.3 libidn/1.15"})


(describe "Wrap-promote-Header headers-to-lowercase"
  (it "prefix and 2 lowercase headers map should return h_etag"
      (= "h_etag" (first (first (#'c/convert-to-lowercase-prefix {"Content-Type" "application/vnd.yousee+json", "ETag" "0"} "h_" )))))
          
  (given [req {:content-type "application/json"
                            :body (stream "")
                            :headers headers
                            :params {"id" 3}}
          resp (json-echo req)]
         (it "All headers should be lowercase and prefixed with header_ "
             (= "header_content-length" (first (first (:params resp )))))))




(describe "is-empty?"
          (it "nil-should-be-false"
              (false? ( c/is-empty? nil)))
          (it "empty-string-should-be-false"
              (false? (c/is-empty? "")))
          (it "Empty-map-should-be-false"
              (false? (c/is-empty? {} )))
          (it "Empty-map-in-a-string-should-be-false"
              (false? (c/is-empty? "{}")))
          (it "String-with-null-as-text-should-be-false"
              (false? (c/is-empty? "null")))
          (it "String-with-nil-as-text-should-be-false"
              (false? (c/is-empty? "nil")))
          (it "String-with-space-should-be-true"
              (true? (c/is-empty? " " )))
          (it "String-with-a-char-should-be-true"
              (true? (c/is-empty? "c")))
          (it "Map with a key and value should be true"
              (true? (c/is-empty? {:foo "bar"})))
          (it "Map with a key and value should be true"
              (true? (c/is-empty? {"foo" "bar"})))
          )

(describe "find-error-code"
          (it "With nil, Should return nil, since no error code is found"
              (nil? (#'c/find-error-code nil )))
          (it "With no-error-code, Should return nil, since no error code is found"
              (nil? (#'c/find-error-code "some-text")))
          (it "With a error code, Should return 123"
              (= " 123 " (#'c/find-error-code "some-text 123 ")))
          (it "With a error code, Should return 122"
              (= " 122 " (#'c/find-error-code "some-text 122 123 123")))
          )
