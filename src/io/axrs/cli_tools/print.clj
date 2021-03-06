(ns io.axrs.cli-tools.print
  (:require
    [io.axrs.cli-tools.ansi :as ansi]))

(defn- ansi-width [v]
  (if (satisfies? ansi/TermText v)
    (ansi/width v)
    (count (ansi/strip (str v)))))

(defn- ansi-value [v]
  (str (if (satisfies? ansi/TermText v)
         (ansi/value v)
         v)))

(defn- padded [width v]
  (let [text-width (ansi-width v)
        v (ansi-value v)
        ansi-offset (- width text-width)
        padding (max width (+ (count v) ansi-offset))]
    (format (str "%-" padding "s") v)))

(defn redln [& args]
  (some->> args
           (apply str)
           ansi/red
           println))

(defn table
  "Prints a collection of maps in a textual table. Code is derived from
  clojure.pprint/print-table, but left-aligned."
  ([ks rows] (table nil ks rows))
  ([formatter ks rows]
   (when (seq rows)
     (let [widths (map
                    (fn [k]
                      (apply max (count (str k)) (map #(ansi-width (get % k)) rows)))
                    ks)
           rows (if formatter
                  (map formatter rows)
                  rows)
           spacers (map #(apply str (repeat % "-")) widths)
           fmt-row (fn [divider row]
                     (apply str (interpose divider
                                  (map padded widths (map #(get row %) ks)))))]
       (println (fmt-row " | " (zipmap ks ks)))
       (println (fmt-row "-|-" (zipmap ks spacers)))
       (doseq [row rows]
         (println (fmt-row " | " row)))))))

(defn clear-screen []
  (print "\033[2J\033[3J\033[1;1H"))
