(ns io.axrs.cli-tools.print
  (:require
    [clojure.string :as str]
    [glow.ansi :as ansi]))

(defn- strip-ansi [s]
  (str/replace s #"\e\[.*?m" ""))

(defn- padded [width v]
  (let [v (str v)
        ansi-offset (- width (count (strip-ansi v)))
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
       (println)
       (println (fmt-row " | " (zipmap ks ks)))
       (println (fmt-row "-+-" (zipmap ks spacers)))
       (doseq [row rows]
         (println (fmt-row " | " row)))))))

