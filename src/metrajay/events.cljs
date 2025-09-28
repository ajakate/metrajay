(ns metrajay.events
  (:require
   ["jszip" :as JSZip]
   [re-frame.core :as rf]
   [superstructor.re-frame.fetch-fx]
   [metrajay.duckdb :as duck]
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
     (js/console.log "is it loaded..." loaded)
     (if (= true loaded)
       {:dispatch [:check-db]}
       (do
         (js/setTimeout #(rf/dispatch [:init-db]) 500)
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
            :url "http://localhost:8787?route=update"
            :mode :cors
            :credentials :omit
            :response-content-types {#"text/plain" :text}
            :on-success [:check-update-value]
            :on-failure [:bad-fetch-result]}}))

(rf/reg-event-fx
 :download-schedule
 (fn [{:keys [db]} event-vec]
   {:fetch {:method :get
            :url "http://localhost:8787?route=schedule"
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
       {:dispatch [:update-last-updated]}
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
   (-> ((duck/load-csvs-func) (clj->js table-csvs) true)
       (.then #(js/console.log "All tables loaded and persisted to IndexedDB.")))
   {}))

(rf/reg-event-fx
 :load-all-stops
 (fn [{:keys [db]} [_ _]]
   {:duckdb {:query "SELECT stop_id, stop_name FROM stops"
             :on-success [:set-available-stations]
             :on-failure [:bad-fetch-result]}}))

(rf/reg-event-fx
 :get-second-station-list
 (fn [{:keys [db]} [_ {:keys [station_id station_name]}]]
   {:alasql {:query "SELECT * FROM stops"
             :on-success [:set-available-stations]
             :on-failure [:bad-fetch-result]}}))

(rf/reg-event-fx
 :bad-fetch-result
 (fn [{:keys [db]} [_ error]]
   {:db (assoc db :error error)}))

(defn find-station-by-name [stops name]
  (first (filter #(= (:stop_name %) name) stops)))

(rf/reg-event-fx
 :set-station-1
 (fn [{:keys [db]} [_ station-name]]
   (let [station-obj (find-station-by-name (:available-stations db) station-name)]
     {:db (assoc db :station-1 station-obj)
      :dispatch [:get-second-station-list station-obj]})))

(persisted-reg-event-db
 :update-last-updated
 (fn [db [_ _]]
   (assoc db :last-updated (js/Date.))))

(persisted-reg-event-db
 :update-update-string
 (fn [db [_ val]]
   (assoc db :update-string val)))

(rf/reg-event-db
 :set-available-stations
 (fn [db [_ val]]
   (assoc db :available-stations val)))


;; Subscriptions
(rf/reg-sub
 :available-stations
 (fn [db _]
   (get db :available-stations [])))
