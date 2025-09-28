(ns metrajay.duckdb)

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


(defn get-db-conn []
  (.-dbConnection js/window))

(defn load-csvs-func []
  (.-loadCsvs js/window))

(defn check-if-kv-exists-func []
  (.-checkIfKVExists js/window))

(defn load-csvs-from-indexeddb-func []
  (.-loadCsvsFromIndexedDB js/window))

(defn query-duckdb [sql]
  (let [conn (get-db-conn)]
    (when conn
      (.then
       (.query conn sql)
       (fn [res]
         (let [rows (.toArray res)
               json-rows (.map rows (fn [x] (.toJSON x)))]
           (js->clj json-rows {:keywordize-keys true})))))))
