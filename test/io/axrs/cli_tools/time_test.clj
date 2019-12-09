(ns io.axrs.cli-tools.time-test
  (:refer-clojure :exclude [=])
  (:require
    [io.jesi.backpack.test.strict :refer :all]
    [clj-time.core :as time-core]
    [io.axrs.cli-tools.time :as time]))

(def now (time-core/now))

(deftest humanized-interval-test

  (testing "humanized-interval"
    (let [humanized= (fn [expected-str seconds]
                       (let [actual (time-core/plus now (time-core/seconds seconds))]
                         (prn (time/humanized-interval now actual))
                         (= expected-str (time/humanized-interval now actual))))]

      (testing "returns 0s if the values are identical"
        (is (humanized= "0s" 0)))

      (testing "order does not matter"
        (let [in-2-hours (time-core/plus now (time-core/hours 2))]
          (is= "2h"
               (time/humanized-interval now in-2-hours)
               (time/humanized-interval in-2-hours now))))

      (testing "intervals"
        (is (humanized= "15s" 15))
        (is (humanized= "59s" 59))
        (is (humanized= "1m" 60))
        (is (humanized= "1m 20s" (+ 60 20)))
        (is (humanized= "50m 29s" (+ (* 50 60) 29)))
        (is (humanized= "1d" (* 24 60 60)))
        (is (humanized= "2d 5h 2m 12s" (+ (* 2 24 60 60)
                                          (* 5 60 60)
                                          (* 2 60)
                                          12)))
        (is (humanized= "175w 6d 8h 1m 32s" (+ (* 1231 24 60 60)
                                               (* 8 60 60)
                                               (* 1 60)
                                               32)))))))
