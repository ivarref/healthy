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

  (with-bindings {#'h/*now-ms*
                  (fn [] (+ (System/currentTimeMillis)
                            (.toMillis (Duration/parse "PT15M"))))}
    (is (= 0 (h/error-count)))

    (h/add-error!)
    (h/add-error!)
    (h/add-error!)
    (is (= 3 (h/error-count)))))
