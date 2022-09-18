(ns com.github.ivarref.healthy2-test
  (:require [clojure.test :refer [deftest is]]
            [com.github.ivarref.healthy2 :as h2])
  (:import (java.time Duration)))

(deftest basics
  (let [now-ms (atom 0)
        x (h2/init {:duration "PT60M"
                    :ok?      #(<= % 1000)
                    :now-ms   (fn [] @now-ms)
                    :healthy? (fn [{:keys [error-percentage]}]
                                (not (>= error-percentage 5)))})]
    (is (true? @x))
    (x 100)
    (is (true? @x))
    (x 10000)
    (is (false? @x))
    (swap! now-ms (partial + (dec (.toMillis (Duration/ofHours 1)))))
    (is (false? @x))
    (swap! now-ms inc)
    (is (true? @x))))

