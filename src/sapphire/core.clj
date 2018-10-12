;
; Copyright (c) 2018 the original author or authors.
; Licensed under the Apache License, Version 2.0 (the "License").
; See LICENSE file in the root directory of this source tree.
;

(ns sapphire.core
  "biu biu biu"
  {:author "olOwOlo"}
  (:import (clojure.lang Symbol Util)))

(defonce
  ^{:private true
    :doc "Get clojure.core private function."}
  core-sigs #'clojure.core/sigs)

(defmacro defcomponent
  {:doc "Based on `clojure.core/defn` and has the same syntax.
  Put your sapphire metadata into attr-map."
   :arglists '([name doc-string? attr-map? [params*] prepost-map? body]
                [name doc-string? attr-map? ([params*] prepost-map? body)+ attr-map?])}
  [name & fdecl]
  (when-not (instance? Symbol name)
    (throw (IllegalArgumentException. "First argument to defcomponent must be a symbol")))
  (let [m (if (string? (first fdecl))
            {:doc (first fdecl)}
            {})
        fdecl (if (string? (first fdecl))
                (next fdecl)
                fdecl)
        m (if (map? (first fdecl))
            (conj m (first fdecl))
            m)
        fdecl (if (map? (first fdecl))
                (next fdecl)
                fdecl)
        fdecl (if (vector? (first fdecl))
                (list fdecl)
                fdecl)
        m (if (map? (last fdecl))
            (conj m (last fdecl))
            m)
        fdecl (if (map? (last fdecl))
                (butlast fdecl)
                fdecl)
        m (conj {:arglists (list 'quote (core-sigs fdecl))} m)
        m (let [inline (:inline m)
                ifn (first inline)
                iname (second inline)]
            ;; same as: (if (and (= 'fn ifn) (not (symbol? iname))) ...)
            (if (if (Util/equiv 'fn ifn)
                  (if (instance? Symbol iname) false true))
              ;; inserts the same fn name to the inline fn if it does not have one
              (assoc m :inline (cons ifn (cons (Symbol/intern (.concat (.getName ^Symbol name) "__inliner"))
                                               (next inline))))
              m))
        m (conj (or (meta name) {}) m)
        options (merge
                  (select-keys (meta *ns*) [:keep-sapphire-meta :cache-defaults])
                  (select-keys m [:keep-sapphire-meta :cache-defaults :cache-result :cache-put :cache-remove :cache-remove-all]))
        m (if (true? (:keep-sapphire-meta m))
            m
            (dissoc m :keep-sapphire-meta :cache-defaults :cache-result :cache-put :cache-remove :cache-remove-all))]
    (list 'def (with-meta name m)
          ;;clojure.to.do - restore propagation of fn name
          ;;must figure out how to convey primitive hints to self calls first
          ;;(cons `fn fdecl)
          (with-meta (list 'sapphire.cache/cache-warp (cons `fn fdecl) options) {:rettag (:tag m)}))))
