(ns metrajay.duckdb
    (:require
     ["idb-keyval" :as idb]
     [metrajay.db :as appdb]))

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

(defn get-duckdb []
  (.-DuckDB js/window))

(defn get-db-conn []
  (.-dbConnection js/window))

(defn load-csv-func []
  (.-loadCsv js/window))

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


(defn load-csv-from-string [[table-name csv-text]]
  (let [conn   (get-db-conn)
        duckdb (get-duckdb)
        filename (str "/tmp/" table-name ".csv")]
    (js/console.log "Loading table:" table-name " into DuckDB...")
    #(-> (.registerFileText duckdb filename csv-text)
         (.then (fn []
                  (.query conn
                          (str "CREATE OR REPLACE TABLE "
                               table-name
                               " AS SELECT * FROM read_csv_auto('"
                               filename "')"))))
         ;; After success, persist the raw CSV string to IndexedDB
         (.then (fn [res]
                  (idb/set table-name csv-text)
                  res)))))  ;; return the result for chaining

(defn load-csv-from-indexeddb [table-name]
  (let [csv-text-promise (idb/get table-name)
        duckdb (get-duckdb)
        conn   (get-db-conn)
        filename (str "/tmp/" table-name ".csv")]
    #(-> csv-text-promise 
         
         (.then (fn [csv-text]
                  (.registerFileText duckdb filename csv-text)))
         (.then (fn [] 
                  (.query conn
                          (str "CREATE OR REPLACE TABLE "
                               table-name
                               " AS SELECT * FROM read_csv_auto('"
                               filename "')")))))))

(defn load-all-tables []
  (let [tables (keys table-fields)]
    (mapv load-csv-from-indexeddb tables)))
