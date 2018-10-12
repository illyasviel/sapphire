;
; Copyright (c) 2018 the original author or authors.
; Licensed under the Apache License, Version 2.0 (the "License").
; See LICENSE file in the root directory of this source tree.
;

(ns sapphire.cache-test
  ;; `:cache-defaults` should be placed in namespace metadata.
  {:cache-defaults {:cache-name "default"}}
  (:require [clojure.test :refer :all]
            [sapphire.core :refer :all]
            [sapphire.cache :as cache]
            [clojure.tools.logging :as log]))

(log/info "Testing with clojure" (clojure-version))

(defcomponent get-from-default
  "Get int by id from default."
  {:cache-result {}}
  [id]
  (log/debug (str "invoke [get-from-default], param " \[ id \]))
  id)

(defcomponent get-from-default-with-value
  {:cache-result {:key cache/first-param}}
  [id value]
  (log/debug (str "invoke [get-from-default-with-value], param " \[ id \, value \]))
  value)

(defcomponent put-into-default
  {:cache-put {:key cache/first-param}}
  [id ignore]
  (log/debug (str "invoke [put-into-default], param " \[ id \, ignore \]))
  id)

(defcomponent remove-from-default
  {:cache-remove {}}
  [id]
  (log/debug (str "invoke [remove-from-default], param " \[ id \])))

(defcomponent remove-all-from-default
  {:cache-remove-all {}}
  []
  (log/debug "invoke [remove-all-from-default]"))

(defcomponent get-from-another
  {:cache-result {:cache-name "another", :key (cache/take-n-params 1), :sync true}}
  [id ignore]
  (log/debug (str "invoke [get-from-another], param " \[ id \, ignore \]))
  id)

(defcomponent get-from-another-with-value
  {:cache-result {:cache-name "another", :key cache/first-param}}
  [id value]
  (log/debug (str "invoke [get-from-another-with-value], param " \[ id \, value \]))
  value)

(defcomponent get-from-another-no-cache
  [id]
  (log/debug (str "invoke [get-from-another-no-cache], param " \[ id \]))
  id)

(defcomponent remove-from-another
  {:cache-remove {:cache-name "another", :key cache/take-all-params}}
  [id]
  (log/debug (str "invoke [remove-from-another], param " \[ id \])))

(defcomponent remove-all-from-another
  {:cache-remove-all {:cache-name "another"}}
  []
  (log/debug "invoke [remove-all-from-another]"))

;; test-ns-hook + deftest

(deftest single-provider-test
  (testing "test :cache-result get missing"
    (is (= 1 (get-from-default 1)))
    (is (= 1 (get-from-default-with-value 1 "other value")))
    (is (= "other value" (get-from-default-with-value 2 "other value")))
    (is (= "other value" (get-from-default 2))))

  (testing "test params"
    (is (= "3" (get-from-default "3")))
    (is (= "3" (get-from-default-with-value "3" "other value")))
    (is (= '(4) (get-from-default '(4))))
    (is (= '(4) (get-from-default-with-value '(4) "other value")))
    (is (= [5] (get-from-default [5])))
    (is (= [5] (get-from-default-with-value [5] "other value")))
    (is (= #{6} (get-from-default #{6})))
    (is (= #{6} (get-from-default-with-value #{6} "other value")))
    (is (= {:7 7} (get-from-default {:7 7})))
    (is (= {:7 7} (get-from-default-with-value {:7 7} "other value")))
    (is (= {:8 {:8 8}} (get-from-default {:8 {:8 8}})))
    (is (= {:8 {:8 8}} (get-from-default-with-value {:8 {:8 8}} "other value"))))

  (testing "test :cache-remove-all"
    (is (= 1 (get-from-default 1)))
    (remove-all-from-default)
    (is (= "other value" (get-from-default-with-value 1 "other value"))))

  (testing "test :cache-put and :key"
    (remove-all-from-default)
    (is (= 1 (put-into-default 1 "ignore")))
    (is (= 1 (get-from-default-with-value 1 "other value"))))

  (testing "test :cache-remove"
    (remove-all-from-default)
    (is (= 1 (get-from-default 1)))
    (remove-from-default 1)
    (is (= "other value" (get-from-default-with-value 1 "other value"))))

  (testing "test without :cache-defaults"
    (testing "test another :cache-result"
      (is (= 1 (get-from-another 1 "ignore")))
      (is (= 1 (get-from-another-with-value 1 "other value"))))

    (testing "test another :cache-remove-all"
      (is (= 1 (get-from-another 1 "ignore")))
      (remove-all-from-another)
      (is (= "other value" (get-from-another-with-value 1 "other value"))))

    (testing "test another no cache"
      (is (= 1 (get-from-another-no-cache 1)))
      (is (= "other value" (get-from-another-with-value 1 "other value")))
      (is (= 1 (get-from-another-no-cache 1))))

    (testing "test another :cache-remove"
      (remove-all-from-another)
      (is (= 1 (get-from-another 1 "ignore")))
      (remove-from-another 1)
      (is (= "other value" (get-from-another-with-value 1 "other value"))))))

(defn test-ns-hook []
  ;; test ehcache 3
  (cache/sapphire-init!
    :cache-manager (cache/jcache-cache-manager-factory
                     :fully-qualified-class-name "org.ehcache.jsr107.EhcacheCachingProvider"
                     :config-file-path "ehcache3.xml"))
  (single-provider-test))
