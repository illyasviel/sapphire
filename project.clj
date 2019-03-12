(defproject sapphire "0.1.0-beta4-SNAPSHOT"
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
            [lein-cloverage "1.1.1"]
            [lein-pprint "1.2.0"]]
  :profiles {:dev {:resource-paths ["resources" "test/resources"]
                   :dependencies [[org.ehcache/ehcache "3.7.0"]
                                  [com.github.ben-manes.caffeine/caffeine "2.7.0"]
                                  [com.github.ben-manes.caffeine/jcache "2.7.0"]
                                  [org.redisson/redisson "3.10.4"]
                                  [org.apache.logging.log4j/log4j-slf4j-impl "2.11.2"]
                                  [pjstadig/humane-test-output "0.9.0"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]}
             :provided {:dependencies [[org.clojure/clojure "1.10.0"]
                                       [javax.xml.bind/jaxb-api "2.3.0"]
                                       [com.sun.xml.bind/jaxb-core "2.3.0"]
                                       [com.sun.xml.bind/jaxb-impl "2.3.0"]
                                       [javax.activation/activation "1.1.1"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo/"
                                    :username :env
                                    :password :env}]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit" "[lein release] prepare release %s"]
                  ["vcs" "tag" "v"]
                  ["deploy" "clojars"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit" "[lein release] prepare for next development iteration"]
                  ["vcs" "push"]])
