(ns metrajay.events
  (:require
   ["jszip" :as JSZip]
   [re-frame.core :as rf]
   [superstructor.re-frame.fetch-fx]
   [metrajay.duckdb :as duck]
   [clojure.string :as str]
   [metrajay.env :as env]
   [akiroz.re-frame.storage :refer [persist-db-keys]]))

(rf/reg-fx
 :duckdb
 (fn [{:keys [query on-success on-failure]}]
   (js/console.log "duckdb query: " query)
   (-> (duck/query-duckdb query)
       (.then #(when on-success (rf/dispatch (conj on-success %))))
       (.catch #(when on-failure (rf/dispatch (conj on-failure %)))))))


(defn persisted-reg-event-db
  [event-id handler]
  (rf/reg-event-fx
   event-id
   [(persist-db-keys :metrajay-app [:update-string :last-updated])]
   (fn [{:keys [db]} event-vec]
     {:db (handler db event-vec)})))

(persisted-reg-event-db :init-local-storage (fn [db] db))

(rf/reg-event-fx
 :init-db
 (fn [{:keys [db]} [_ _]]
   (let [loaded (exists? (.-duckdbLoaded js/window))]
     (js/console.log "Checking if DuckDB is loaded..." loaded)
     (if (= true loaded)
       {:dispatch [:check-db]}
       (do
         (js/setTimeout #(rf/dispatch [:init-db]) 200)
         {})))))

(rf/reg-event-fx
 :load-db-from-indexeddb
 (fn [{:keys [db]} [_ _]]
   (let [table-names (keys duck/table-fields)]
     (-> ((duck/load-csvs-from-indexeddb-func) (clj->js table-names))
         (.then #(rf/dispatch [:check-for-update])))
     {})))

(rf/reg-event-fx
 :check-db
 (fn [{:keys [db]} _]
   (let [table-names (keys duck/table-fields)]
     (-> ((duck/check-if-kv-exists-func) (clj->js table-names))
         (.then (fn [result]
                  (if
                   (= true result)
                    (rf/dispatch [:load-db-from-indexeddb])
                    (rf/dispatch [:download-schedule])))))
     {})))


(rf/reg-event-fx
 :check-for-update
 (fn [{:keys [db]} event-vec]
   {:fetch {:method :get
            :url (str env/API_URL "?route=update")
            :mode :cors
            :credentials :omit
            :response-content-types {#"text/plain" :text}
            :on-success [:check-update-value]
            :on-failure [:bad-fetch-result]}}))

(rf/reg-event-fx
 :download-schedule
 (fn [{:keys [db]} event-vec]
   {:fetch {:method :get
            :url (str env/API_URL "?route=schedule")
            :mode :cors
            :credentials :omit
            :response-content-types {#"application/zip" :array-buffer}
            :on-success [:zip-success]
            :on-failure [:bad-fetch-result]}}))

(rf/reg-event-fx
 :bad-fetch-result
 (fn [{:keys [db]} [_ response]]
   (js/console.log "Bad fetch result: " (:body response))
   (js/console.log "Full response obj: " response)))

(rf/reg-event-fx
 :check-update-value
 (fn [{:keys [db]} [_ response]]
   (let [body (:body response)
         current-string (:update-string db)]
     (if (= body current-string)
       {:fx [[:disptach [:update-last-updated]] [:dispatch [:load-all-stops]]]}
       {:fx [[:disptach [:update-update-string body]] [:dispatch [:download-schedule]]]}))))

(rf/reg-event-db
 :set-debug
 (fn [db [_ resp]]
   (assoc db :debug resp)))

(rf/reg-event-fx
 :zip-success
 (fn [_ [_ response]]
   (let [zip (JSZip.)
         body (:body response)]
     (js/console.log "Unzipping..." body)
     (-> (.loadAsync zip body)
         (.then (fn [unzipped]
                  (js/Promise.all
                   (for [[filename _] duck/table-map]
                     (.. unzipped (file filename) (async "string"))))))
         (.then (fn [texts]
                  (rf/dispatch [:load-new-db
                                (map vector (vals duck/table-map) texts)])))))
   {}))


(rf/reg-event-fx
 :load-new-db
 (fn [{:keys [db]} [_ table-csvs]]
   (js/console.log "Loading CSVs into DB sequentially...")
   (-> ((duck/load-csvs-func) (clj->js table-csvs))
       (.then #(js/console.log "All tables loaded and persisted to IndexedDB."))
       (.then (rf/dispatch [:load-all-stops])))
   {}))

(rf/reg-event-fx
 :load-all-stops
 (fn [{:keys [db]} [_ _]]
   {:duckdb {:query duck/all-stations-query
             :on-success [:set-all-stops]
             :on-failure [:bad-fetch-result]}}))

(rf/reg-event-fx
 :get-second-station-list
 (fn [{:keys [db]} [_ station_obj]]
   (let [station-id (:stop_id station_obj)
         query (duck/second-stations-query station-id)] 
     {:duckdb {:query query
               :on-success [:set-available-stations-2]
               :on-failure [:bad-fetch-result]}})))

(rf/reg-event-fx
 :get-schedule
 (fn [{:keys [db]} [_ _]]
   (let [station1 (-> db :station-1 :stop_id)
         station2 (-> db :station-2 :stop_id)
         query (duck/schedule-query station1 station2)]
     {:duckdb {:query query
               :on-success [:set-schedule]
               :on-failure [:bad-fetch-result]}})))

(rf/reg-event-fx
 :bad-fetch-result
 (fn [{:keys [db]} [_ error]]
   {:db (assoc db :error error)}))

(defn find-station-by-name [stops name]
  (first (filter #(= (:stop_name %) name) stops)))


(rf/reg-event-fx
 :set-station-1
 (fn [{:keys [db]} [_ station-name]]
   {:db (assoc db :station-1 station-name)
    :dispatch [:get-second-station-list]}))

(rf/reg-event-fx
 :set-station-2
 (fn [{:keys [db]} [_ station-name]]
   {:db (assoc db :station-2 station-name)}))

(persisted-reg-event-db
 :update-last-updated
 (fn [db [_ _]]
   (assoc db :last-updated (js/Date.))))

(persisted-reg-event-db
 :update-update-string
 (fn [db [_ val]]
   (assoc db :update-string val)))

(rf/reg-event-db
 :set-all-stops
 (fn [db [_ val]]
   (assoc db :all-stops val)))

(rf/reg-event-db
 :set-available-stations
 (fn [db [_ val]]
   (assoc db :available-stations val)))

(defn timekey [day-group]
  (let [time-strings (mapv #(get % :time1) day-group)]
    (str/join ";" time-strings)))

(defn group-schedule [grouped-day]
  (partition-by timekey grouped-day))

(defn group-day [raw-times] 
  (partition-by #(:schedule_day %) raw-times))

(rf/reg-event-db
 :set-schedule
 (fn [db [_ val]]
   (let [grouped-day (group-day val)
         grouped-schedule (group-schedule grouped-day)]
     (assoc db :schedule grouped-schedule))))

(rf/reg-event-db
 :set-available-stations-2
 (fn [db [_ val]]
   (assoc db :available-stations-2 val)))

(rf/reg-event-db
 :set-sample-query
 (fn [db [_ val]]
   (assoc db :sample-query val)))

(rf/reg-event-fx
 :run-sample-query
 (fn [{:keys [db]} [_ query]]
   {:duckdb {:query query
             :on-success [:set-sample-query]
             :on-failure [:bad-fetch-result]}}))




;; Subscriptions

(rf/reg-sub
 :all-stops
 (fn [db _]
   (get db :all-stops [])))

(rf/reg-sub
 :available-stations
 :<- [:all-stops]
 (fn [all-stops _]
   (let [stop-names  (mapv #(get % :stop_name) all-stops)
         distinct-stops (distinct stop-names)
         sorted (sort distinct-stops)]
     sorted)))


(defn element-in-array [obj arr]
  (some #(= obj %) arr))

(rf/reg-sub
 :available-stations-2
 :<- [:all-stops]
 :<- [:station-1]
 (fn [[all-stops station-1] _]
   (let [all-obj-for-station (filter #(= (:stop_name %) station-1) all-stops)
         all-routes (mapv #(:route_id %) all-obj-for-station)
         all-stations-for-route (filter #(element-in-array (get % :route_id) all-routes) all-stops)
         all-names (mapv #(get % :stop_name) all-stations-for-route)
         distinct-stops (distinct all-names)
         sorted (sort distinct-stops)]
     sorted)))

(rf/reg-sub
 :station-1
 (fn [db _]
   (get db :station-1)))

(rf/reg-sub
 :station-2
 (fn [db _]
   (get db :station-2)))

(rf/reg-sub
 :can-submit-search
 :<- [:station-1]
 :<- [:station-2]
 (fn [[station-1 station-2] _]
   (and (seq station-1) (seq station-2))))

(rf/reg-sub
 :sample-query
 (fn [db _]
   (get db :sample-query [])))
