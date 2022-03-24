(ns com.github.ivarref.healthy
  (:import (java.time Duration)))

(def config (atom {}))
(def errors (atom {}))

(def ^:dynamic *now-ms* (fn [] (System/currentTimeMillis)))

(declare drop-old-errors-impl! add-error!-impl)

(defn init! [{:keys [duration]
              :as _cfg}]
  (let [duration (if (string? duration)
                   (Duration/parse duration)
                   duration)]
    (assert (instance? Duration duration) "Expected :duration to be of type java.time.Duration")
    (let [millis (.toMillis ^Duration duration)]
      (assert (pos-int? millis) "Duration must be positive/non-zero")
      (assert (>= millis 60e3) "Duration must be at least 60 seconds")
      (reset! config {:duration millis})
      (reset! errors {}))))

(defn error-count []
  (reduce + 0 (vals (drop-old-errors-impl! @config (*now-ms*) errors))))

(defn add-error! []
  (add-error!-impl @config (*now-ms*) errors))


(defn- remove-old [duration now-ms errs]
  (when (nil? duration)
    (throw (ex-info "Duration was nil, did you forget to call `com.github.ivarref.healthy/init!`?" {})))
  (reduce-kv (fn [m k v]
               (if (<= k (- now-ms duration))
                 m
                 (assoc m k v)))
             {}
             errs))

(defn add-error!-impl [{:keys [duration]} now-ms err-atom]
  (let [bucket (long (* 60e3 (long (/ now-ms 60e3))))]
    (swap! err-atom
           (fn [errs]
             (->> (update errs bucket (fnil inc 0))
                  (remove-old duration now-ms))))))

(defn drop-old-errors-impl! [{:keys [duration]} now-ms err-atom]
  (swap! err-atom (partial remove-old duration now-ms)))
