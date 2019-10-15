(ns io.axrs.cli-tools.env
  (:refer-clojure :exclude [get])
  (:require
    [clojure.string :as str]))

(defn get [v]
  (let [v (System/getenv v)]
    (when-not (str/blank? v)
      v)))
