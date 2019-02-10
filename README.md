# sapphire

[![Build Status](https://travis-ci.org/illyasviel/sapphire.svg?branch=master)](https://travis-ci.org/illyasviel/sapphire)
[![Coverage Status](https://coveralls.io/repos/github/illyasviel/sapphire/badge.svg?branch=master)](https://coveralls.io/github/illyasviel/sapphire?branch=master)
[![Clojars Project](https://img.shields.io/clojars/v/sapphire.svg)](https://clojars.org/sapphire)
[![Dependencies Status](https://versions.deps.co/illyasviel/sapphire/status.svg)](https://versions.deps.co/illyasviel/sapphire)
[![Downloads](https://versions.deps.co/illyasviel/sapphire/downloads.svg)](https://versions.deps.co/illyasviel/sapphire)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

A clojure declarative cache library inspired by Spring Cache and JCache.

## Feature

- [JCache](https://jcp.org/en/jsr/detail?id=107) (JSR-107)
  - [EhCache 3](http://www.ehcache.org/)
  - [Caffeine](https://github.com/ben-manes/caffeine)
  - and more...

## Usage

-  Adding the following to your `:dependencies`

```clojure
[sapphire "0.1.0-beta1"]
```

- Currently we only support JCache, so you need to choose a provider
and add it to your `:dependencies`

```clojure
[org.ehcache/ehcache "3.6.3"]
```

- Init cache

```clojure
(ns sapphire.example
  (:require [sapphire.core :refer :all]
            [sapphire.cache :as cache]))

;; ehcache 3
(cache/sapphire-init!
  :cache-manager (cache/jcache-cache-manager-factory
                   :fully-qualified-class-name "org.ehcache.jsr107.EhcacheCachingProvider"
                   :config-file-path "ehcache3.xml")) ;; <-- Provider also need a configuration file.
```

- Cache Result (`:cache-result`)

```clojure
(defcomponent find-user-by-id
  "The result is cacheable."
  {:cache-result {:cache-name "user"}}
  [id]
  (prn "Get from database.."))
;; => #'user/find-user-by-id
(find-user-by-id 666)
;; "Get from database.."
;; => nil
(find-user-by-id 666)
;; => nil
```

- Cache Put (`:cache-put`)

```clojure
(defcomponent put-user-into-cache
  "Explicit put the data into cache."
  {:cache-put {:cache-name "user", :key cache/first-param}}
  [id user]
  (prn "Put the result into cache.")
  user)
;; => #'user/put-user-into-cache
(put-user-into-cache 777 {:username "777"})
;; "Put the result into cache."
;; => {:username "777"}
(find-user-by-id 777)
;; => {:username "777"}
```

- Cache Remove By Key (`:cache-remove`)

```clojure
(defcomponent remove-user-from-cache
  "Remove data from cache."
  {:cache-remove {:cache-name "user", :key cache/take-all-params}}
  [id]
  (prn "Remove data from cache by id."))
;; => #'user/remove-user-from-cache
(remove-user-from-cache 777)
;; "Remove data from cache by id."
;; => nil
(find-user-by-id 777)
;; "Get from database.."
;; => nil
```

- Cache Remove All  (`:cache-remove-all`)

```clojure
(defcomponent remove-all-user-from-cache
  "Remove data from cache."
  {:cache-remove-all {:cache-name "user"}}
  []
  (prn "Remove all data from cache."))
;; => #'user/remove-all-user-from-cache
(remove-all-user-from-cache)
;; "Remove all data from cache."
;; => nil
(find-user-by-id 666)
;; "Get from database.."
;; => nil
```

- Default cache metadata  (`:cache-defaults`)

```clojure
(ns sapphire.example
  "You can offer default cache config for all cache components in current namespace."
  {:cache-defaults {:cache-name "user"}}
  (:require [sapphire.core :refer :all]
            [sapphire.cache :as cache]))
```

- Keep sapphire metadata

By default, sapphire's metadata will not be retained.
Unless you specify `:keep-sapphire-meta` on the current namespace **or** function.

```clojure
(ns sapphire.example
  {:keep-sapphire-meta true}
  (:require [sapphire.core :refer :all]
            [sapphire.cache :as cache]))
            
(defcomponent keep-sapphire-metadata
  {:keep-sapphire-meta true}
  []
  (prn "Do something."))
```

## Documents

### Macro

Macro | Description
----- | -----------
`defcomponent` | Based on `clojure.core/defn` and has the same syntax. Put the sapphire metadata into attr-map.

### Metadata

Metadata Key | Description
--- | ---
<a name="keep-sapphire-meta"><a/>`:keep-sapphire-meta` | `true`. By default, sapphire's metadata will not be retained. Unless you specify `:keep-sapphire-meta` on the current namespace **or** function
<a name="cache-defaults"></a>`:cache-defaults` | `{}`. Specify the default cache config for current namespace.
<a name="cache-defaults-cache-name"></a> | `{:cache-name ""}`. Default cache name.
<a name="cache-defaults-key-generator"></a> | `{:key-generator func}`. Default key generator.
<a name="cache-result"></a>`:cache-result` | `{}`. Cache the result for current function by key.
<a name="cache-result-cache-name"></a> | `{:cache-name ""}`. Specify the cache name used to get cache from cache manager.
<a name="cache-result-key"></a> | `{:key func}`. Specify the function used to take params that will be passed to key generator.
<a name="cache-result-key-generator"></a> | `{:key-generator func}`. Specify the function used to generate key. `(key-generator (key args))`
<a name="cache-result-sync"></a> | `{:sync true}`. Synchronize the invocation of the underlying method if several threads are attempting to load a value for the same key. This is effectively a hint and the actual cache provider that you are using may not support it in a synchronized fashion.
<a name="cache-put"></a>`:cache-put` | `{}`. Explicitly put the result into cache by key.
<a name="cache-put-cache-name"></a> | `{:cache-name ""}`.
<a name="cache-put-key"></a> | `{:key func}`.
<a name="cache-put-key-generator"></a> | `{:key-generator func}`.
<a name="cache-remove"></a>`:cache-remove` | `{}`. Explicitly remove data from cache by key.
<a name="cache-remove-cache-name"></a> | `{:cache-name ""}`.
<a name="cache-remove-key"></a> | `{:key func}`.
<a name="cache-remove-key-generator"></a> | `{:key-generator func}`. 
<a name="cache-remove-all"></a>`:cache-remove-all` | `{}`. Explicitly remove all data from cache.
<a name="cache-remove-all-cache-name"></a> | `{:cache-name ""}`.
endAndKeepThisTableWidth | endAndKeepThisTableWidth

### Built-in key func

Func | Description
---- | -----------
`cache/take-all-params` | The **default** key func, take all params as key.
`cache/first-param` | Take the first param as key.
`(cache/take-n-params n)` | Take n params as key.

