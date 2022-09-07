# healthy

A simple Clojure (JVM) library for doing health checks over a given duration.

## Installation

[![Clojars Project](https://img.shields.io/clojars/v/com.github.ivarref/healthy.svg)](https://clojars.org/com.github.ivarref/healthy)

## 1-minute example

```clojure
(require '[com.github.ivarref.healthy :as h])

; Initialize. Set the duration to keep errors for to be 1 minute:
(h/init! {:duration "PT1M"})
; :duration is given as a ISO 8601 duration: 
; https://en.wikipedia.org/wiki/ISO_8601#Durations
; :duration must be at least 60 seconds.

; Add an error:
(h/add-error!)
; => 1 ; error count

; Add another error:
(h/add-error!)
; => 2 ; error count

(h/add-error!)
; => 3 ; error count

; Add an OK event:
(h/add-ok!)
; => 1 ; ok count

; Get error percentage:
(h/error-percentage)
; => 75.0

; Or alternatively get the error count:
(h/error-count)
; => 3

; Wait 1 minute. Then get error percentage (or error count) again:
(h/error-percentage)
; Yes, this function changes state though there is no bang!
; => 0.0
```

## Example usage for HTTP server backends

The idea is that you may for example want to invoke `(h/add-error!)` if a request takes
too long to finish or fails in some unexpected way. 
Invoke `(h/add-ok!)` otherwise if the request is fine.

Your `/health` endpoint or similar may return healthy (or not) based on `(h/error-percentage)`
like the following:

```clojure
(defn healthcheck [{:keys [response]}]
  (let [error-percentage (h/error-percentage)]
    (if (>= error-percentage 5.0) ; if more than 5% of requests failed, return unhealthy
      (do
        (log/warn "Error percentage is" error-percentage ", returning unhealthy")
        (assoc response
          :status 503
          :body {:status "Unhealthy, too many errors"}))
      (assoc response
        :status 200
        :body {:status "OK"}))))
```

If you use this example, make sure that `(h/add-error!)` is *not* invoked 
when `/health` returns unhealthy, otherwise your service may not be able to recover.
Yes, I did that mistake ¯\\\_(ツ)\_/¯.

## Change log

#### 2022-09-07 v0.1.17 [diff](https://github.com/ivarref/healthy/compare/v0.1.16...v0.1.17)
Add concept of error-percentage.

Changed internal bucket storage size from 60 seconds to 1 second.

#### 2022-03-24 v0.1.16
Initial public release.

## License

Copyright © 2022 Ivar Refsdal

This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary Licenses when the conditions for such availability set forth in the Eclipse Public License, v. 2.0 are satisfied: GNU General Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any later version, with the GNU Classpath Exception which is available at https://www.gnu.org/software/classpath/license.html.
