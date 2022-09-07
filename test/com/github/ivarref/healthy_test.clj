(ns com.github.ivarref.healthy-test
  (:require [clojure.test :refer [deftest is]]
            [com.github.ivarref.healthy :as h])
  (:import (java.time Duration)))

(deftest basic
  (h/init! {:duration "PT15M"})
  (is (= 0 (h/error-count)))

  (h/add-error!)
  (is (= 1 (h/error-count)))

  (h/add-error!)
  (is (= 2 (h/error-count)))
  (is (= 100.0 (h/error-percentage)))

  (with-bindings {#'h/*now-ms*
                  (fn [] (+ (System/currentTimeMillis)
                            (.toMillis (Duration/parse "PT15M"))))}
    (is (= 0 (h/error-count)))
    (is (= 0.0 (h/error-percentage)))

    (h/add-error!)
    (h/add-error!)
    (h/add-error!)
    (h/add-ok!)
    (is (= 3 (h/error-count)))
    (is (= 75.0 (h/error-percentage)))))


(def add (partial #'h/add-to-bucket 60e3))

(deftest err-percentage
  (is (= {1000 {:error 1}, 10000 {:error 1}, 20000 {:error 1}, 60000 {:error 1}}
         (->> (add :error 1000 {})
              (add :error 10000)
              (add :error 20000)
              (add :error 60000))))

  (is (= {1000 {:error 2 :ok 2}}
         (->> (add :error 1000 {})
              (add :error 1001)
              (add :ok 1000)
              (add :ok 1001))))

  (is (= 50.0 (->> (add :error 1000 {})
                   (add :ok 1000)
                   (#'h/sum-stats-map)
                   (:error-percentage)))))
