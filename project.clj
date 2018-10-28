(defproject sapphire "0.1.0-beta1"
  :description "A clojure declarative cache library inspired by Spring Cache and JCache."
  :url "https://github.com/illyasviel/sapphire"
  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo}
  :scm {:name "git" :url "https://github.com/illyasviel/sapphire"}
  :global-vars {*warn-on-reflection* true}
  :javac-options ["-source" "8" "-target" "8" "-g"]
  :dependencies [[org.clojure/tools.logging "0.4.1"]
                 [javax.cache/cache-api "1.1.0"]]
  :exclusions [org.clojure/clojure]
  :plugins [[lein-kibit "0.1.6"]
            [lein-cloverage "1.0.13"]
            [lein-pprint "1.2.0"]]
  :profiles {:dev {:resource-paths ["resources" "test/resources"]
                   :dependencies [[org.ehcache/ehcache "3.6.1"]
                                  [org.apache.logging.log4j/log4j-slf4j-impl "2.11.1"]]}
             :provided {:dependencies [[org.clojure/clojure "1.9.0"]
                                       [javax.xml.bind/jaxb-api "2.3.0"]
                                       [com.sun.xml.bind/jaxb-core "2.3.0"]
                                       [com.sun.xml.bind/jaxb-impl "2.3.0"]
                                       [javax.activation/activation "1.1.1"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo/"
                                    :username :env
                                    :password :env}]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit" "[lein release] prepare release %s"]
                  ["vcs" "tag"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit" "[lein release] prepare for next development iteration"]
                  ["vcs" "push"]])
