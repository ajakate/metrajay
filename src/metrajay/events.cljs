(ns metrajay.events
  (:require
   ["jszip" :as JSZip]
   [re-frame.core :as rf]
   [superstructor.re-frame.fetch-fx]
   [metrajay.duckdb :as duck]
   [clojure.string :as str]
   [clojure.set :as set]
   [metrajay.env :as env]
   [akiroz.re-frame.storage :refer [persist-db-keys]]
   [reitit.frontend.controllers :as rfc]
   [reitit.frontend.easy :as rfe]
   [metrajay.util :as u]))

(def easy-persisted
  [:update-string
   :last-updated
   :all-stops])

(def easy-events
  [:query-1
   :query-2
   :schedule
   :sample-query
   :all-stops])

(defn persisted-reg-event-db
  [event-id handler]
  (rf/reg-event-fx
   event-id
   [(persist-db-keys :metrajay-app (conj easy-persisted :favorites))]
   (fn [{:keys [db]} event-vec]
     {:db (handler db event-vec)})))

(persisted-reg-event-db :init-local-storage (fn [db] db))


(doseq [value easy-persisted]
  (let [update-name (keyword (str "set-" (name value)))]
    (persisted-reg-event-db
     update-name
     (fn [db [_ val]]
       (assoc db value val)))))

(doseq [value easy-events]
  (let [update-name (keyword (str "set-" (name value)))]
    (rf/reg-event-db
     update-name
     (fn [db [_ val]]
       (assoc db value val)))))

(rf/reg-fx
 :duckdb
 (fn [{:keys [query on-success on-failure]}]
   (js/console.log "duckdb query: " query)
   (-> (duck/query-duckdb query)
       (.then #(when on-success (rf/dispatch (into on-success [%]))))
       (.catch #(when on-failure (rf/dispatch (into on-failure [%])))))))


(rf/reg-event-db
 :navigated
 (fn [db [_ match]]
   (let [old-match (:current-route db)
         controllers (rfc/apply-controllers (:controllers old-match) match)]
     (assoc db :current-route (assoc match :controllers controllers)))))


(rf/reg-event-fx
 :init-db
 (fn [{:keys [db]} [_ _]]
   (let [loaded (exists? (.-duckdbLoaded js/window))]
     (js/console.log "Waiting for duckdb to initialize..." loaded)
     (if (= true loaded)
       {:dispatch [:check-db]}
       {:dispatch-later {:ms 200 :dispatch [:init-db]}}))))

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
       {:fx [[:disptach [:set-last-updated (js/Date.)]] [:dispatch [:load-all-stops]]]}
       {:fx [[:disptach [:set-update-string body]] [:dispatch [:download-schedule]]]}))))


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
 (fn [{:keys [db]} [_ [_ _]]]
   (let [loaded (.-dbLoaded js/window)]
     (if loaded
       {:duckdb {:query duck/all-stations-query
                 :on-success [:set-all-stops]
                 :on-failure [:bad-fetch-result]}}
       {:dispatch-later {:ms 200 :dispatch [:load-all-stops]}}))))

(rf/reg-event-fx
 :get-schedule
 (fn [{:keys [db]} [_ [station1 station2]]]
   (let [loaded (.-dbLoaded js/window)
         query (duck/schedule-query station1 station2)]
     (if loaded
       {:duckdb {:query query
                 :on-success [:format-schedule station1 station2]
                 :on-failure [:bad-fetch-result]}
        :db (assoc db :loading-schedule true)}
       {:dispatch-later {:ms 200 :dispatch [:get-schedule [station1 station2]]}
        :db (assoc db :loading-schedule true)}))))


(rf/reg-event-fx
 :init-db
 (fn [{:keys [db]} [_ _]]
   (let [loaded (exists? (.-duckdbLoaded js/window))]
     (if (= true loaded)
       {:dispatch [:check-db]}
       {:dispatch-later {:ms 200 :dispatch [:init-db]}}))))

(rf/reg-event-fx
 :bad-fetch-result
 (fn [{:keys [db]} [_ error]]
   {:db (assoc db :error error)}))

(defn get-obj-for-station-name [station-name, all-stops]
  (let [first-matching (first (filter #(= station-name (:stop_name %)) all-stops))]
    (select-keys first-matching [:stop_id :stop_name])))

(defn get-obj-for-station-id [station-id, all-stops]
  (let [first-matching (first (filter #(= station-id (:stop_id %)) all-stops))]
    (select-keys first-matching [:stop_id :stop_name])))

(rf/reg-event-fx
 :set-station-1
 (fn [{:keys [db]} [_ station-name]]
   (let [all-stops (:all-stops db)]
     {:db (assoc db :station-1 (get-obj-for-station-name station-name all-stops))})))

(rf/reg-event-fx
 :set-station-2
 (fn [{:keys [db]} [_ station-name]]
   (let [all-stops (:all-stops db)]
     {:db (assoc db :station-2 (get-obj-for-station-name station-name all-stops))})))

(rf/reg-event-fx
 :clear-schedule
 (fn [{:keys [db]} [_ _]]
   {:db (assoc db :schedule nil)}))

(rf/reg-event-fx
 :navigate-to-schedule
 (fn [{:keys [db]} [_ val]]
   (let [all-stops (:all-stops db)
         station1-name (-> db :station-1 :stop_name)
         station2-name (-> db :station-2 :stop_name)
         station1-objs (filter #(= station1-name (get % :stop_name)) all-stops)
         station2-objs (filter #(= station2-name (get % :stop_name)) all-stops)
         station1-routes (set (mapv #(:route_id %) station1-objs))
         station2-routes (set (mapv #(:route_id %) station2-objs))
         routes (set/intersection station1-routes station2-routes)
         station1 (first (filter #(contains? routes (:route_id %)) station1-objs))
         station2 (first (filter #(= (:route_id station1) (:route_id %)) station2-objs))
         result (if (>
                     (int (:stop_sequence station1))
                     (int (:stop_sequence station2)))
                  [station1 station2]
                  [station2 station1])]
     (rfe/push-state :schedule {}
                     {:stop1 (-> result first :stop_id)
                      :stop2 (-> result second :stop_id)}))))

(defn timekey [day-group]
  (let [time-strings (mapv #(str (:time1 %) ";" (:time2 %)) day-group)]
    (str/join ";" time-strings)))

(defn get-day-name [_ group]
  (let [weekday-list  (map #(-> % first :weekday) group)
        sorted (sort-by u/day-order weekday-list)
        compressed (u/compress-days sorted)]
    [compressed (first group)]))

(defn format-schedule-group [coll]
  (let [by-day (partition-by :schedule_day coll)
        grouped (group-by timekey by-day)
        update-keys (u/remap-keys-and-vals grouped get-day-name)]
    update-keys))

(rf/reg-event-fx
 :format-schedule
 (fn [{:keys [db]} [_ station1 station2 val]]
   (let [station-1 (get-obj-for-station-id station1 (:all-stops db))
         station-2 (get-obj-for-station-id station2 (:all-stops db))
         by-direction (group-by :direction_id val)
         formatted {:inbound (format-schedule-group (get by-direction "inbound"))
                    :outbound (format-schedule-group (get by-direction "outbound"))
                    :station1 station-1
                    :station2 station-2}]
     {:dispatch [:set-schedule formatted]
      :db (assoc db :loading-schedule false)})))

(rf/reg-event-fx
 :run-sample-query
 (fn [{:keys [db]} [_ query]]
   {:duckdb {:query query
             :on-success [:set-sample-query]
             :on-failure [:bad-fetch-result]}}))

(rf/reg-event-db
 :clear-search-selection
 (fn [db [_ _]]
   (assoc db :station-1 nil :station-2 nil :query-1 "" :query-2 "")))

(persisted-reg-event-db
 :add-to-favorites
 (fn [db [_ _]]
   (let [favorites (get db :favorites [])
         current-route [(-> db :schedule :station1) (-> db :schedule :station2)]]
     (assoc db :favorites (conj favorites current-route)))))

(persisted-reg-event-db
 :remove-from-favorites
 (fn [db [_ val]]
   (let [favorites (get db :favorites [])
         without-val (filter #(not= val %) favorites)]
     (assoc db :favorites without-val))))


;; Subscriptions ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; --
;; Subscriptions ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; --
;; Subscriptions ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; --
;; Subscriptions ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; --
;; Subscriptions ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; -- ;; --


(def easy-subs
  [:all-stops
   :current-route
   :station-1
   :station-2
   :query-1
   :query-2
   :schedule
   :favorites
   :loading-schedule
   :sample-query])

(doseq [event easy-subs]
  (rf/reg-sub
   event
   (fn [db _]
     (-> db event))))

(rf/reg-sub
 :current-view
 (fn [db _] (-> db :current-route :data :view)))

(rf/reg-sub
 :available-stations
 :<- [:all-stops]
 (fn [all-stops _]
   (let [stop-names  (mapv #(get % :stop_name) all-stops)
         distinct-stops (distinct stop-names)
         sorted (sort distinct-stops)]
     sorted)))

(rf/reg-sub
 :available-stations-2
 :<- [:all-stops]
 :<- [:station-1]
 (fn [[all-stops station-1] _]
   (let [all-obj-for-station (filter #(= (:stop_id %) (:stop_id station-1)) all-stops)
         all-routes (set (mapv #(:route_id %) all-obj-for-station))
         all-stations-for-route (filter #(contains? all-routes (get % :route_id)) all-stops)
         all-names (mapv #(get % :stop_name) all-stations-for-route)
         distinct-stops (distinct all-names)
         exclude-self (filter #(not= (:stop_name station-1) %) distinct-stops)
         sorted (sort exclude-self)]
     sorted)))

(rf/reg-sub
 :can-submit-search
 :<- [:station-1]
 :<- [:station-2]
 (fn [[station-1 station-2] _]
   (and (seq station-1) (seq station-2))))

(rf/reg-sub
 :can-add-to-favorites
 :<- [:favorites]
 :<- [:schedule]
 (fn [[favorites schedule] _]
   (let [schedule-hash (str (-> schedule :station1 :stop_id)  (-> schedule :station2 :stop_id))
         favorites-map (set (mapv #(str (:stop_id (first %)) (:stop_id (second %))) favorites))]
     (not (contains? favorites-map schedule-hash)))))
