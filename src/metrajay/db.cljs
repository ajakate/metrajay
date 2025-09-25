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

(defn init-db []
  (-> (.promise js/alasql "CREATE INDEXEDDB DATABASE IF NOT EXISTS metrajay;")
      (.then (fn [_] (.promise js/alasql "ATTACH INDEXEDDB DATABASE metrajay;")))
      (.then (fn [_] (.promise js/alasql "USE metrajay;")))
      (.then (fn [_] (.promise js/alasql "create table if not exists calendar_dates;")))
      (.then (fn [_] (.promise js/alasql "create table if not exists calendar;")))
      (.then (fn [_] (.promise js/alasql "create table if not exists routes;")))
      (.then (fn [_] (.promise js/alasql "create table if not exists stop_times;")))
      (.then (fn [_] (.promise js/alasql "create table if not exists stops;")))
      (.then (fn [_] (.promise js/alasql "create table if not exists trips;")))
      (.then (fn [_] (js/console.log "done")))
      (.catch (fn [err] (js/console.error "DB init error:" err)))))
