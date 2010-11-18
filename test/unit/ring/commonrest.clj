(ns unit.ring.commonrest 
  (:require [ring.commonrest :as c])
  (:use
          [lazytest.describe :only (describe before after it given do-it for-any with)])
  )

(describe is-empty? 
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

(describe find-error-code
          (it "With nil, Should return nil, since no error code is found"
              (nil? (#'c/find-error-code nil )))
          (it "With no-error-code, Should return nil, since no error code is found"
              (nil? (#'c/find-error-code "some-text")))
          (it "With a error code, Should return 123"
              (= " 123 " (#'c/find-error-code "some-text 123 ")))
          (it "With a error code, Should return 122"
              (= " 122 " (#'c/find-error-code "some-text 122 123 123")))
          )
