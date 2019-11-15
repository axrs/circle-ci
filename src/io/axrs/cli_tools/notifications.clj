(ns io.axrs.cli-tools.notifications
  (:refer-clojure :exclude [send])
  (:require [clojure.java.shell :as sh]))

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

(defn send [{:keys [message title subtitle url] :as notification}]
  (or
    (terminal-notifier notification)
    (osascript notification)))
