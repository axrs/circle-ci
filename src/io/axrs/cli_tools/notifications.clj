(ns io.axrs.cli-tools.notifications
  (:refer-clojure :exclude [send])
  (:require
    [io.jesi.backpack.macros :refer [def-]]
    [clojure.java.shell :as sh]
    [clojure.string :as string]))

(defn- clean-exit? [{:keys [exit]}]
  (zero? exit))

(defn- program-exists?
  [s]
  (clean-exit? (sh/sh "sh" "-c" (format "command -v %s" s))))

(defn- terminal-notifier [{:keys [message title url subtitle]}]
  (when (program-exists? "terminal-notifier")
    (let [cmd ["terminal-notifier" "-message" message "-title" title]
          cmd (if subtitle
                (concat cmd ["-subtitle" subtitle])
                cmd)
          cmd (if url
                (concat cmd ["-open" url])
                cmd)]
      (clean-exit? (apply sh/sh cmd)))))

(defn- osascript [{:keys [message title subtitle]}]
  (when (program-exists? "osascript")
    (clean-exit? (sh/sh "osascript" "-e" (str "display notification \"" message "\" with title \"" title "\""
                                           (when subtitle (str " subtitle \"" subtitle "\"")))))))

(def- non-blank? (complement string/blank?))

(defn- optional-non-empty-string [s]
  (or (nil? s)
      (non-blank? s)))

(defn send [{:keys [message title subtitle url] :as notification}]
  {:pre [(non-blank? message)
         (non-blank? title)
         (optional-non-empty-string subtitle)
         (optional-non-empty-string url)]}
  (or
    (terminal-notifier notification)
    (osascript notification)))
