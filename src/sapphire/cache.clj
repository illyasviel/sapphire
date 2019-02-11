;
; Copyright (c) 2018 the original author or authors.
; Licensed under the Apache License, Version 2.0 (the "License").
; See LICENSE file in the root directory of this source tree.
;

(ns sapphire.cache
  "biu biu biu"
  {:author "olOwOlo"}
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io])
  (:import (javax.cache Caching CacheManager Cache)
           (javax.cache.processor EntryProcessor)
           (clojure.lang ILookup PersistentHashMap PersistentHashSet)
           (java.io Closeable)))

(defprotocol SapphireCache
  "This is the protocol describing the basic cache capability."
  (get-name [c]
    "Return the cache name.")
  (get-native-cache [c]
    "Return the underlying native cache provider.")
  (lookup [c key]
    "Return the value(nilable) to which this cache maps the specified key.
    Get the raw value by `(from-store-value (lookup cache key))`.
    The nil value represents the value that is not found in the cache.")
  (retrieve! [c key ^Callable value-loader]
    "Return the value to which this cache maps the specified key,
    obtaining that value from `value-loader` if necessary.")
  (put! [c key value]
    "Associate the specified value with the specified key in this cache.")
  (put-if-absent! [c key value]
    "Atomically associate the specified value with the specified key in this cache
     if it is not set already.")
  (remove! [c key]
    "Removes the mapping for a key from this cache if it is present.")
  (remove-all! [c]
    "Removes all of the mappings from this cache.
    Notifying listeners and cache writers if necessary."))

(defprotocol SapphireCacheManager
  "This is the protocol manage sapphire cache."
  (init-caches! [cm]
    "Initialize caches on startup.")
  (get-cache! [cm name]
    "Return the cache associated with the given name.")
  (get-cache-names [cm]
    "Return a collection of the cache names known by this manager."))

(defrecord SimpleCacheKey [args])
(defonce empty-cache-key (->SimpleCacheKey []))

(defn simple-key-generator [args]
  (if (nil? args)
    empty-cache-key
    (->SimpleCacheKey args)))

(defn take-all-params
  "Select all params as key, this is the default key fn."
  [args]
  (apply list args))

(defn take-n-params
  "Return a key fn take n params as key."
  [n]
  (fn [args]
    (apply list (take n args))))

(defonce first-param (take-n-params 1))

(defrecord NilValue [])
(defonce nil-value (->NilValue))

(defn to-store-value
  "nil -> nil-value"
  [value]
  (if (nil? value)
    nil-value
    value))

(defn from-store-value
  "nil-value -> nil"
  [value]
  (if (= nil-value value)
    nil
    value))

(def ^SapphireCacheManager default-cache-manager nil)
(def default-key-generator simple-key-generator)
(def default-key take-all-params)

(defn- generate-cache-key
  "`args`: [& args] arguments seq
  `options`: the options
  `action`: :cache-result|:cache-put|:cache-remove|:cache-remove-all"
  [options action args]
  (let [key-generator (or (get-in options [action :key-generator])
                          (get-in options [:cache-defaults :key-generator])
                          default-key-generator)
        key (or (get-in options [action :key])
                default-key)]
    (log/trace (str "Generate key by selected args '" (key args) "' result '" (key-generator (key args)) "' when '" action \'))
    (key-generator (key args))))

(defn- resolve-cache
  "Determines the `cache` to use for an intercepted method invocation.
  Return nil if no cache found or no cache is needed."
  [options action]
  (when (some? (action options))
    (let [cache-name (or (get-in options [action :cache-name])
                         (get-in options [:cache-defaults :cache-name])
                         (throw (IllegalStateException. (str "Cannot get cache name for " \' action \'))))
          cache-manager default-cache-manager
          cache (get-cache! cache-manager cache-name)]
      (when (nil? cache)
        (log/warn "\"Cannot find cache named " \' cache-name \'))
      cache)))


(defn cache-warp
  "Higher order function."
  [f options]
  (fn [& args]
    (if (true? (get-in options [:cache-result :sync]))
      ;; handle sync
      (when-let [cache (resolve-cache options :cache-result)]
        (retrieve! cache (generate-cache-key options :cache-result args) #(apply f args)))
      ;; handle normal
      ;; find cached warped result if necessary
      (let [cached-warped-rst (when-let [cache (resolve-cache options :cache-result)]
                                (get cache (generate-cache-key options :cache-result args)))
            rst (if (some? cached-warped-rst)
                  (do
                    (log/trace (str "cache '" (get-name (resolve-cache options :cache-result)) "' hit by key"))
                    (from-store-value cached-warped-rst))
                  (apply f args))]
        ;; process :cache-result miss
        (when-let [cache (resolve-cache options :cache-result)]
          (when (nil? cached-warped-rst)
            (log/trace (str "cache '" (get-name cache) "' put(miss) by key"))
            (put! cache (generate-cache-key options :cache-result args) (to-store-value rst))))
        ;; process explicit :cache-put
        (when-let [cache (resolve-cache options :cache-put)]
          (log/trace (str "cache '" (get-name cache) "' put(explicit) by key"))
          (put! cache (generate-cache-key options :cache-put args) (to-store-value rst)))
        ;; process :cache-remove
        (when-let [cache (resolve-cache options :cache-remove)]
          (log/trace (str "cache '" (get-name cache) "' remove by key"))
          (remove! cache (generate-cache-key options :cache-remove args)))
        ;; process :cache-remove-all
        (when-let [cache (resolve-cache options :cache-remove-all)]
          (log/trace (str "cache '" (get-name cache) "' remove all"))
          (remove-all! cache))
        ;; return
        rst))))


(defn sapphire-init!
  "Init something"
  [& {:keys [cache-manager]}]
  (log/info "Init sapphire cache...")
  (alter-var-root #'default-cache-manager (constantly cache-manager))
  (init-caches! default-cache-manager)
  (log/info "Set global default cache manager success."))


;; JCache implements

(deftype ValueLoaderEntryProcessor []
  EntryProcessor
  (process [_ entry arguments]
    (let [^Callable value-loader (first arguments)]
      (if (.exists entry)
        (from-store-value (.getValue entry))
        (let [value (.call value-loader)]
          (.setValue entry (to-store-value value))
          value)))))

(deftype JCacheCache [^Cache cache]
  SapphireCache
  (get-name [_]
    (.getName cache))
  (get-native-cache [_]
    cache)
  (lookup [_ key]
    (.get cache key))
  (retrieve! [_ key value-loader]
    (.invoke cache key (->ValueLoaderEntryProcessor) (to-array (vector value-loader))))
  (put! [_ key value]
    (.put cache key value))
  (put-if-absent! [_ key value]
    (.putIfAbsent cache key value))
  (remove! [_ key]
    (.remove cache key))
  (remove-all! [_]
    (.removeAll cache))

  ILookup
  (valAt [this key]
    (lookup this key))
  (valAt [this key not-found]
    (if-let [result (lookup this key)]
      result
      not-found))

  Closeable
  (close [_]
    (.close cache)))

(deftype JCacheCacheManager [^CacheManager cache-manager
                             ^:volatile-mutable ^PersistentHashMap cache-map
                             ^:volatile-mutable ^PersistentHashSet cache-names]
  SapphireCacheManager
  (init-caches! [this]
    (let [tran-cache-map (transient (hash-map))
          tran-cache-names (transient (hash-set))]
      (doseq [cache-name (.getCacheNames cache-manager)]
        (log/trace (str "[init-caches] init cache '" cache-name \'))
        (conj! tran-cache-names cache-name)
        (assoc! tran-cache-map cache-name (->JCacheCache (.getCache cache-manager cache-name))))
      (locking this
        (set! cache-names (persistent! tran-cache-names))
        (set! cache-map (persistent! tran-cache-map)))))
  (get-cache! [this name]
    (if-let [cache (.get cache-map name)]
      cache
      ;; get missing cache
      (locking this
        (if-let [cache (.get cache-map name)]
          cache
          (let [cache (.getCache cache-manager name)
                warped-cache (->JCacheCache cache)]
            (when (some? cache)
              (log/trace (str "Load cache " \[ name \] " which created at runtime."))
              (set! cache-names (conj cache-names name))
              (set! cache-map (assoc cache-map name warped-cache))
              warped-cache))))))
  (get-cache-names [_]
    cache-names)

  Closeable
  (close [_]
    (.close cache-manager)))

(defn jcache-cache-manager-factory
  "Return a JCacheCacheManager."
  [& {:keys [^String fully-qualified-class-name
             ^String config-file-path]}]
  (let [provider (if (some? fully-qualified-class-name)
                   (Caching/getCachingProvider fully-qualified-class-name)
                   (Caching/getCachingProvider))
        native-cache-manager (.getCacheManager provider
                                               (some-> config-file-path (io/resource) (.toURI))
                                               (-> empty-cache-key (.getClass) (.getClassLoader)))
        jcache-cache-manager (->JCacheCacheManager native-cache-manager (hash-map) (hash-set))]
    jcache-cache-manager))

;; ./End JCache implements

(defn shutdown-manager
  "Shut down the given/default cache manager."
  ([]
   (shutdown-manager default-cache-manager))
  ([cache-manager]
   (when (instance? Closeable cache-manager)
     (.close ^Closeable cache-manager))))
