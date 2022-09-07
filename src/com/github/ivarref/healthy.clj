(ns com.github.ivarref.healthy
  (:import (java.time Duration)))

(def ^:private config (atom {}))
(def ^:private stats (atom {}))

(def ^:private ^:dynamic *now-ms* (fn [] (System/currentTimeMillis)))

(declare drop-old-errors-impl! sum-stats-map add-to-bucket)

(defn init! [{:keys [duration]
              :as   _cfg}]
  (let [duration (if (string? duration)
                   (Duration/parse duration)
                   duration)]
    (assert (instance? Duration duration) "Expected :duration to be of type java.time.Duration")
    (let [millis (.toMillis ^Duration duration)]
      (assert (pos-int? millis) "Duration must be positive/non-zero")
      (assert (>= millis 60e3) "Duration must be at least 60 seconds")
      (reset! config {:duration millis})
      (reset! stats {}))))

(defn add-error!
  "Adds an error. Possibly remove old data.

  Returns number of errors."
  []
  (:error (sum-stats-map (swap! stats (partial add-to-bucket (:duration @config) :error (*now-ms*))))))

(defn add-ok!
  "Adds an OK event. Possibly remove old data.

  Returns number of OK events."
  []
  (:ok (sum-stats-map (swap! stats (partial add-to-bucket (:duration @config) :ok (*now-ms*))))))

(defn error-count
  "Returns number of errors. Possibly remove old data."
  []
  (:error (sum-stats-map (drop-old-errors-impl! @config (*now-ms*) stats))))

(defn error-percentage
  "Returns error percentage, a double ranging from 0 to 100. Possibly remove old data."
  []
  (:error-percentage (sum-stats-map (drop-old-errors-impl! @config (*now-ms*) stats))))

(defn- sum-stats-map [m]
  (let [{:keys [ok error]
         :or   {ok 0 error 0}} (reduce (partial merge-with +) {} (vals m))]
    {:ok               ok
     :error            error
     :error-percentage (if (and (= ok 0) (= error 0))
                         0.0
                         (double (* 100 (/ error (+ ok error)))))}))

(defn- remove-old [duration now-ms errs]
  (when (nil? duration)
    (throw (ex-info "Duration was nil, did you forget to call `com.github.ivarref.healthy/init!`?" {})))
  (reduce-kv (fn [m k v]
               (if (<= k (- now-ms duration))
                 m
                 (assoc m k v)))
             {}
             errs))

(defn- add-to-bucket [duration bucket-type now-ms stats]
  (let [bucket (long (* 1e3 (long (/ now-ms 1e3))))]
    (->> (update-in stats [bucket bucket-type] (fnil inc 0))
         (remove-old duration now-ms))))

(defn- drop-old-errors-impl! [{:keys [duration]} now-ms stats-atom]
  (swap! stats-atom (partial remove-old duration now-ms)))
