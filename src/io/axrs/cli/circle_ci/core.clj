(ns io.axrs.cli.circle-ci.core
  (:require
    [cli-matic.core :as cli]
    [com.rpl.specter :as sp]
    [io.axrs.cli-tools.ansi :as ansi]
    [io.axrs.cli-tools.notifications :as notification]
    [io.axrs.cli-tools.env :as env]
    [io.axrs.cli-tools.http :as http]
    [io.axrs.cli-tools.print :as print]
    [io.axrs.cli-tools.time :as time]
    [io.jesi.backpack :as bp]
    [slingshot.slingshot :refer [try+ throw+]]
    [taoensso.encore :refer [assoc-some]]))

(defonce ^:private project-urls (atom {}))

(defn- token []
  (env/get "CIRCLECI_TOKEN"))

(defn- get-projects []
  (try+
    (:body (http/get-json "https://circleci.com/api/v1.1/projects"
             {:circle-token (token)}))
    (catch http/client-error? {:as response}
      (http/print-error response "Please ensure your CIRCLE_CI token is correct")
      (throw+))))

(defn- get-project-url [project]
  (if-let [url (get @project-urls project)]
    url
    (let [urls (reduce
                 (fn [m {:keys [username vcs-type reponame]}]
                   (assoc m reponame (str "https://circleci.com/api/v1.1/project/" vcs-type \/ username \/ reponame)))
                 {}
                 (get-projects))]
      (reset! project-urls urls)
      (get @project-urls project))))

(defn- get-recent [{:keys [limit project]}]
  (try+
    (when-let [url (if project
                     (get-project-url project)
                     "https://circleci.com/api/v1.1/recent-builds")]
      (:body (http/get-json url
               (assoc-some {:circle-token (token)}
                 :limit (some-> limit (min 100))))))
    (catch http/client-error? {:as response}
      (http/print-error response "Please ensure your CIRCLE_CI token is correct")
      (throw+))))

(defn- utc-dates->local [{:keys [start-time stop-time queued-at build-url workflow-url] :as result} now]
  (let [[start-time stop-time queued-at] (map time/->date-time [start-time stop-time queued-at])]
    (assoc result
      :build-link (ansi/hyperlink build-url "Build")
      :workflow-link (ansi/hyperlink workflow-url "Workflow")
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

(defn- filter-by-params [{:keys [branch job-name]} results]
  (let [job-name? (key-match? :job-name job-name)
        branch? (key-match? :branch branch)]
    (filter #(and (branch? %)
                  (job-name? %))
      results)))

(defonce ^:private default-cols
  [:status :queued-at :run-time :reponame :branch :job-name :subject :build-link :workflow-link])

(defonce ^:private notified (atom #{}))

(defn- recent [{:keys [project cols extra-cols watch notify]
                :or   {cols default-cols}
                :as   params}]
  (let [watch? (some-> watch pos?)
        notify? (and watch? (some-> notify pos?))
        cols (if project (remove (bp/p= :reponame) cols) cols)]
    (when watch?
      (print/clear-screen))
    (let [results (->> (get-recent params)
                       clean-results
                       (filter-by-params params))]
      (when notify?
        (doseq [{:keys [reponame branch job-name run-time build-url subject]} (filter (comp (bp/p= "failed") :status) results)]
          (when-not (contains? @notified build-url)
            (notification/send {:message  subject
                                :subtitle (str job-name " failed (" run-time ")")
                                :url      build-url
                                :title    (str reponame " | " branch)})
            (swap! notified conj build-url))))
      (print/table colorize (concat cols extra-cols) results))
    (when watch?
      (Thread/sleep (* 1000 watch))
      (recur params))))

(defn- projects [{:as params}]
  (->> (get-projects)
       (sort-by :reponame)
       (print/table colorize [:username :reponame :vcs-type :vcs-url])))

(defn- cols [{:as params}]
  (->> (get-recent {:limit 1})
       first
       keys
       (concat [:build-link :workflow-link])
       sort
       clojure.pprint/pprint))

(defonce ^:private cli-config
  {:app      {:command     "circle-ci"
              :description "A CircleCI CLI"
              :version     "0.2.0"}
   :commands [
              {:command     "cols"
               :description ["Prints a list of columns available for use in tabular outputs"]
               :runs        cols}
              {:command     "projects"
               :description ["Prints a list of projects followed by the current CircleCI User"]
               :runs        projects}
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
                              :type   :edn}
                             {:option "notify"
                              :as     "Display system notification for failed builds"
                              :type   :int}
                             {:option "watch"
                              :as     "The number of seconds to wait before refresh"
                              :type   :int}]
               :runs        recent}]})

(defn -main [& args]
  (if-not (token)
    (print/redln "No CIRCLECI_TOKEN defined.")
    (cli/run-cmd args cli-config)))
