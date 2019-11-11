(defproject io.axrs.cli/circle-ci "0.2.0"
  :description "A CircleCI CLI"
  :license "Eclipse Public License - v 2.0"
  :url "https://github.com/axrs/circle-ci"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cli-matic "0.3.6" :exclusions [org.clojure/clojure]]
                 [clj-time "0.15.0"]
                 [com.taoensso/encore "2.108.1" :exclusions [org.clojure/clojure]]
                 [io.jesi/backpack "3.6.0" :exclusions [clojure-complete
                                                        com.lucasbradstreet/cljs-uuid-utils
                                                        medley
                                                        nrepl
                                                        org.clojars.mmb90/cljs-cache
                                                        org.clojure/clojure
                                                        org.clojure/core.async
                                                        org.clojure/core.cache
                                                        pjstadig/humane-test-output]]
                 [org.martinklepsch/clj-http-lite "0.4.1" :exclusions [org.clojure/clojure]]]
  :deploy-repositories [["clojars" {:url           "https://clojars.org/repo"
                                    :sign-releases false}]])
