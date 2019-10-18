(defproject io.axrs.cli/circle-ci "0.0.1"
  :description "A CircleCI CLI"
  :license "Eclipse Public License - v 2.0"
  :url "https://github.com/axrs/circle-ci"
  :main io.axrs.cli.circle-ci.core
  :profiles {:uberjar {:aot :all}}
  :dependencies [[cli-matic "0.3.6"]
                 [clj-time "0.15.0"]
                 [com.taoensso/encore "2.108.1"]
                 [io.jesi/backpack "3.4.1"]
                 [org.clojure/clojure "1.10.1"]
                 [org.martinklepsch/clj-http-lite "0.4.1"]]
  :deploy-repositories [["clojars" {:url           "https://clojars.org/repo"
                                    :sign-releases false}]])
