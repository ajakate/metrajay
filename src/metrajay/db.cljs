(ns metrajay.db)

(def table-map
  {"calendar_dates.txt"  "calendar_dates"
   "calendar.txt"        "calendar"
   "routes.txt"         "routes"
   "stop_times.txt"     "stop_times"
   "stops.txt"          "stops"
   "trips.txt"          "trips"})

(def table-fields
  {"calendar_dates" ["service_id" "date" "exception_type"]
   "calendar" ["service_id" "monday" "tuesday" "wednesday" "thursday" "friday" "saturday" "sunday" "start_date" "end_date"]
   "routes" ["route_id" "route_short_name" "route_long_name"]
   "stop_times" ["trip_id" "arrival_time" "departure_time" "stop_id" "stop_sequence"]
   "stops" ["stop_id" "stop_name"]
   "trips" ["route_id" "service_id" "trip_id" "trip_headsign" "direction_id"]})

;; TODO: delete maybe?
(defn run-commands
  "Takes a seq of SQL strings (or functions) and runs them in order as promises.
   Returns a promise that resolves when all are done."
  [cmds]
  (reduce
   (fn [p cmd]
     (.then p
            (fn [_]
              (if (string? cmd)
                (.promise js/alasql cmd)
                (cmd)))))   ;; allow passing fns too, e.g. #(js/console.log "done")
   (js/Promise.resolve)  ;; initial resolved promise
   cmds))

(defn db-reset-commands [version]
  [(str "CREATE INDEXEDDB DATABASE IF NOT EXISTS metrajay_v" version ";")
   (str "ATTACH INDEXEDDB DATABASE metrajay_v" version ";")
   (str "USE metrajay_v" version ";")
   "create table if not exists calendar_dates;"
   "create table if not exists calendar;"
   "create table if not exists routes;"
   "create table if not exists stop_times;"
   "create table if not exists stops;"
   "create table if not exists trips;"]
  )
