(ns io.axrs.cli-tools.print
  (:require
    [io.axrs.cli-tools.ansi :as ansi]))

(defn- padded [width v]
  (let [v (str v)
        ansi-offset (- width (count (ansi/strip v)))
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
                      (apply max (count (str k)) (map #(count (str (get % k))) rows)))
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
  ;Clear
  (print (str (char 27) "[2J"))
  ;Move cursor to start
  (print (str (char 27) "[;H")))
