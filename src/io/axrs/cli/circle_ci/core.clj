(ns io.axrs.cli.circle-ci.core
  (:require
    [cli-matic.core :as cli]
    [com.rpl.specter :as sp]
    [io.axrs.cli-tools.ansi :as ansi]
    [io.axrs.cli-tools.env :as env]
    [io.axrs.cli-tools.http :as http]
    [io.axrs.cli-tools.print :as print]
    [io.axrs.cli-tools.time :as time]
    [io.jesi.backpack :as bp]
    [slingshot.slingshot :refer [try+ throw+]]
    [taoensso.encore :refer [assoc-some]])
  (:gen-class))

(defn- token []
  (env/get "CIRCLECI_TOKEN"))

(defn- get-recent [& [limit]]
  (try+
    (http/get-json "https://circleci.com/api/v1.1/recent-builds"
      (assoc-some {:circle-token (token)}
        :limit (some-> limit (min 100))))
    (catch http/client-error? {:as response}
      (http/print-error response "Please ensure your CIRCLE_CI token is correct")
      (throw+))))

(defn- utc-dates->local [{:keys [start-time stop-time queued-at] :as result} now]
  (let [[start-time stop-time queued-at] (map time/->date-time [start-time stop-time queued-at])]
    (assoc result
      :run-time (some-> start-time (time/humanized-interval (or stop-time now)))
      :start-time (time/->wall-str start-time)
      :stop-time (time/->wall-str stop-time)
      :queued-at (time/->wall-str queued-at))))

(defn- clean-result [now {{:keys [workflow-id] :as workflows} :workflows
                          :as                                 result}]
  (-> result
      (dissoc :workflows)
      (merge workflows)
      (assoc :workflow-url (format "https://circleci.com/workflow-run/%s" workflow-id))
      (utc-dates->local now)))

(defn- clean-results [results]
  (let [now (time/now)]
    (->> results
         (map (partial clean-result now))
         (map (bp/partial-right dissoc :circle-yml)))))

(defn- colorize-status [status]
  (let [color-fn (cond
                   (= "failed" status) ansi/red
                   (= "success" status) ansi/green
                   (= "running" status) ansi/blue
                   :else identity)]
    (color-fn status)))

(defn- colorize [{:as row}]
  (sp/transform [:status some?] colorize-status row))

(defn- key-match? [k v]
  (if v
    (comp (partial = v) k)
    any?))

(defn- filter-by-params [{:keys [project branch job-name]} results]
  (let [project? (key-match? :reponame project)
        job-name? (key-match? :job-name job-name)
        branch? (key-match? :branch branch)]
    (filter #(and (project? %)
                  (branch? %)
                  (job-name? %))
      results)))

(defonce ^:private default-cols
  [:status :queued-at :run-time :reponame :branch :job-name :subject :build-url])

(defn- recent [{:keys [limit cols extra-cols]
                :or   {cols default-cols}
                :as   params}]
  (->> (get-recent limit)
       :body
       clean-results
       (filter-by-params params)
       (print/table colorize (concat cols extra-cols))))

(defn- cols [{:as params}]
  (->> (get-recent 1)
       :body
       first
       keys
       sort
       clojure.pprint/pprint))

(defonce ^:private cli-config
  {:app      {:command     "circle-ci"
              :description "A CircleCI CLI"
              :version     "0.0.1"}
   :commands [{:command     "cols"
               :description ["Prints a list of columns available for use in tabular outputs"]
               :runs        cols}
              {:command     "recent"
               :short       "r"
               :description ["Prints a tabular list of recent jobs"]
               :opts        [{:option  "limit"
                              :as      "The total number of jobs to fetch before filtering (max is 100)."
                              :type    :int
                              :default 25}
                             {:option "project"
                              :as     "Filters the results to a specific reponame"
                              :type   :string}
                             {:option "job-name"
                              :as     "Filters the results to a specific CircleCI Job name"
                              :type   :string}
                             {:option "branch"
                              :as     "Filters the results to a specific branch"
                              :type   :string}
                             {:option "cols"
                              :as     "Columns to print"
                              :type   :edn}
                             {:option "extra-cols"
                              :as     "Extra columns to print (appended to the end of cols)"
                              :type   :edn}]
               :runs        recent}]})

(defn -main [& args]
  (if-not (token)
    (print/redln "No CIRCLECI_TOKEN defined.")
    (cli/run-cmd args cli-config)))
