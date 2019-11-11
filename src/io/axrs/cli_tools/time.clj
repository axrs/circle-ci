(ns io.axrs.cli-tools.time
  (:require
    [clj-time.coerce :as c]
    [clj-time.core :as t]
    [clj-time.format :as f]
    [clojure.string :as string]))

(def seconds-in-minute 60)
(def seconds-in-hour (* 60 seconds-in-minute))
(def seconds-in-day (* 24 seconds-in-hour))
(def seconds-in-week (* 7 seconds-in-day))

(def now t/now)

(defn ->unix [t]
  (-> (c/to-long t)
      (quot 1000)))

(defn add-days [n t]
  (t/plus t (t/days n)))

(defn from-utc-seconds [seconds]
  (c/from-long (* 1000 seconds)))

(defn ->wall-str [utc-date-time]
  (when utc-date-time
    (f/unparse
      (f/formatter-local "yyyy-MM-dd HH:mm:ss")
      (t/to-time-zone utc-date-time (t/default-time-zone)))))

(defn ->date-time [str]
  (some-> str f/parse))

(defn seconds->duration [seconds]
  (let [weeks ((juxt quot rem) seconds seconds-in-week)
        wk (first weeks)
        days ((juxt quot rem) (last weeks) seconds-in-day)
        d (first days)
        hours ((juxt quot rem) (last days) seconds-in-hour)
        hr (first hours)
        min (quot (last hours) seconds-in-minute)
        sec (rem (last hours) seconds-in-minute)]
    (string/join ", "
      (filter #(not (string/blank? %))
        (conj []
          (when (> wk 0) (str wk " wk"))
          (when (> d 0) (str d " d"))
          (when (> hr 0) (str hr " hr"))
          (when (> min 0) (str min " min"))
          (when (> sec 0) (str sec " sec")))))))

(defn humanized-interval [start-date end-date]
  (-> start-date
      (t/interval end-date)
      (t/in-seconds)
      seconds->duration))

(defn start-of-day [date-time]
  (t/with-time-at-start-of-day date-time))

(defn end-of-day [date-time]
  (some->> date-time (add-days 1) start-of-day))

