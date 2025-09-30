(ns metrajay.duckdb
  (:require
   [clojure.string :as str]))


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

(defn render-template
  "Replace {{var}} placeholders in `template` with values from `params`."
  [template params]
  (str/replace
   template
   #"\{\{(\w+)\}\}"
   (fn [[_ k]]
     (str (get params (keyword k) "")))))

(def all-stations-query
  "
select s.stop_id, s.stop_name, t.route_id, group_concat(distinct stop_sequence) as stop_sequence
from stop_times st
join stops s on st.stop_id=s.stop_id
join trips t on t.trip_id=st.trip_id
where t.direction_id = 0
group by s.stop_id, s.stop_name,t.route_id
   "
  
  )

(def second-stations-sql
  "
with distinct_trips as (
  select distinct trip_id from stop_times
  where stop_id = '{{stop_id}}'
),
stop_ids as (
 select distinct stop_id from stop_times where trip_id in (select * from distinct_trips)
)
select stop_id,stop_name
from stops where stop_id in
(select * from stop_ids)
and stop_id != '{{stop_id}}'
   ")

(def get-schedule-sql
  "
with base as (
    select
    st.stop_id as stop_id1,
    st.stop_sequence as stop_sequence1,
    st2.stop_sequence as stop_sequence2,
    s1.stop_name as stop_name1,
    st2.stop_id as stop_id2,
    s2.stop_name as stop_name2,
    st.arrival_time as time1,
    st2.arrival_time as time2,
    t.route_id,
    t.direction_id,
    STRPTIME(LPAD(cast(c.start_date as varchar), 8, '0'), '%Y%m%d') as start_date,
    STRPTIME(LPAD(cast(c.end_date as varchar), 8, '0'), '%Y%m%d') as end_date,
    concat_ws(
    '|',
    CASE WHEN monday = 1 THEN 'Mon' END,
    CASE WHEN tuesday = 1 THEN 'Tue' END,
    CASE WHEN wednesday = 1 THEN 'Wed' END,
    CASE WHEN thursday = 1 THEN 'Thu' END,
    CASE WHEN friday = 1 THEN 'Fri' END,
    CASE WHEN saturday = 1 THEN 'Sat' END,
    CASE WHEN sunday = 1 THEN 'Sun' END
    ) as valid_days,
    STRPTIME(LPAD(cast(cd.date as varchar), 8, '0'), '%Y%m%d') ex_date,
    cd.exception_type ex_type

    from stop_times st
    join trips t on t.trip_id = st.trip_id
    join stop_times st2 on st2.trip_id = t.trip_id
    join calendar c on c.service_id=t.service_id
    left join calendar_dates cd on cd.service_id=t.service_id
    left join stops s1 on st.stop_id = s1.stop_id
    left join stops s2 on st2.stop_id = s2.stop_id
    where st.stop_id = '{{stop_id1}}'
    and st2.stop_id = '{{stop_id2}}'
    and STRPTIME(LPAD(cast(c.end_date as varchar), 8, '0'), '%Y%m%d') >= CURRENT_DATE - 1
    and STRPTIME(LPAD(cast(c.start_date as varchar), 8, '0'), '%Y%m%d') < CURRENT_DATE + 40
    order by t.service_id, direction_id, st.departure_time
)
, days as (
    select CURRENT_DATE + i AS schedule_day,
    STRFTIME(CURRENT_DATE + i, '%a') AS weekday
    FROM UNNEST([0,1,2,3,4,5,6]) AS t(i)
)

select
    strftime(d.schedule_day, '%Y-%m-%d') as schedule_day,
d.weekday,
b.stop_id1,
b.stop_id2,
b.stop_name1,
b.stop_name2,
b.stop_sequence1,
b.stop_sequence2,
b.time1,
b.time2,
b.direction_id,
b.ex_type,
b.ex_type

from days d
    join base b on (d.schedule_day between b.start_date and b.end_date)
where POSITION(d.weekday in b.valid_days) > 0
  and (
    (ex_date is NULL) or
    ((ex_type = 2) and (ex_date != d.schedule_day)))
order by d.schedule_day asc, b.direction_id asc, b.time1 asc
")


"   
    where st.stop_id = '{{stop_id1}}'
    and st2.stop_id = '{{stop_id2}}'
 
 "


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
    (js/console.log "Running query: " sql)
    (when conn
      (.then
       (.query conn sql)
       (fn [res]
         (let [rows (.toArray res)
               json-rows (.map rows (fn [x] (.toJSON x)))]
           (js->clj json-rows {:keywordize-keys true})))))))


(defn second-stations-query [first-station-id]
  (render-template second-stations-sql {:stop_id first-station-id}))

(defn schedule-query [first-station-id, second-station-id]
  (render-template get-schedule-sql {:stop_id1 first-station-id :stop_id2 second-station-id}))
