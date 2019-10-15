(ns io.axrs.cli-tools.http
  (:require
    [clj-http.lite.client :as client]
    [clojure.string :as str]
    [io.axrs.cli-tools.print :as print]
    [io.jesi.backpack :as bp]
    [taoensso.encore :refer [assoc-some]]))

(defn get-json
  ([url] (get-json url nil nil))
  ([url {:as query-params}] (get-json url {} query-params))
  ([url opts {:as query-params}]
   (-> (client/get
         (str url (when query-params "?"))
         (assoc-some (merge {:accept        :json
                             :cookie-policy :none}
                       opts)
           :query-params query-params))
       (update :body bp/json->clj))))

(defn print-error [{:keys [status body] :as response} & error-lines]
  (print/redln "HTTP ERROR: " status \newline \tab
    (str/join (str \newline \tab) error-lines)
    \newline \tab body))

(defn client-error? [{:keys [status] :as request}]
  (<= 400 status 499))
