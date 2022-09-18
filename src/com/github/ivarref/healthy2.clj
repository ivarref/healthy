(ns com.github.ivarref.healthy2
  (:require [com.github.ivarref.healthy :as h])
  (:import (clojure.lang IDeref IFn)
           (java.time Duration)))

(def ^:private sum-stats-map #'h/sum-stats-map)
(def ^:private remove-old #'h/remove-old)
(def ^:private add-to-bucket #'h/add-to-bucket)

(defn init
  [{:keys [duration atom-store now-ms ok? healthy?]
    :or   {atom-store (atom {})
           now-ms     (fn [] (System/currentTimeMillis))}}]
  (assert (fn? ok?) "Expected :ok? to be a function")
  (assert (fn? healthy?) "Expected :healthy? to be a function")
  (let [duration (if (string? duration)
                   (Duration/parse duration)
                   duration)
        _ (assert (instance? Duration duration) "Expected :duration to be of type java.time.Duration")
        duration (.toMillis duration)]
    (reify
      IFn
      (invoke [_ value]
        (swap! atom-store (partial add-to-bucket duration (if (ok? value) :ok :error) (now-ms))))
      IDeref
      (deref [_]
        (healthy? (sum-stats-map (swap! atom-store (partial remove-old duration (now-ms)))))))))
