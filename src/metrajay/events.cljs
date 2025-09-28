(ns metrajay.events
  (:require
   ["jszip" :as JSZip]
   [re-frame.core :as rf]
   [superstructor.re-frame.fetch-fx]
   [metrajay.db :as appdb]
   [clojure.string :as str]
   [akiroz.re-frame.storage :refer [persist-db-keys]]))


(rf/reg-fx
 :alasql
 (fn [{:keys [query on-success on-failure]}]
   (let [p (.promise js/alasql query)]
     (.then p
            (fn [result]
              (when on-success
                (rf/dispatch
                 (conj on-success (js->clj result :keywordize-keys true))))))
     (.catch p
             (fn [error]
               (when on-failure
                 (rf/dispatch
                  (conj on-failure error))))))))

(defn persisted-reg-event-db
  [event-id handler]
  (rf/reg-event-fx
   event-id
   [(persist-db-keys :metrajay-app [:update-string :db-version :last-updated])]
   (fn [{:keys [db]} event-vec]
     {:db (handler db event-vec)})))

(persisted-reg-event-db :init-local-storage (fn [db] db))

(rf/reg-event-fx
 :init-db
 (fn [{:keys [db]} [_ _]]
   (let [raw-version (:db-version db)
         version (if (nil? raw-version) 0 raw-version)
         reset-commands (appdb/db-reset-commands version)
         full-commands (-> reset-commands
                           (conj #(rf/dispatch [:update-db-version version]))
                           (conj #(rf/dispatch [:check-for-update])))]
     (appdb/run-commands full-commands)
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
                   (for [[filename _] appdb/table-map]
                     (.. unzipped (file filename) (async "string"))))))
         (.then (fn [texts]
                  (rf/dispatch [:increment-db-and-load-csvs
                                (map vector (vals appdb/table-map) texts)])))))
   {}))

(rf/reg-event-fx
 :increment-db-and-load-csvs
 (fn [{:keys [db]} [_ table-csvs]]
   (let [new-db-version (inc (:db-version db))
         commands (appdb/db-reset-commands new-db-version)
         full-commands (-> commands
                           (conj #(rf/dispatch [:update-db-version new-db-version]))
                           (conj #(rf/dispatch [:load-new-db table-csvs])))]
     (appdb/run-commands full-commands)
     {})))


(defn map-entry-to-command [table-csv]
  (let [[table-name csv] table-csv]
    (js/console.log "Loading " table-name " into DB...")
    #(js/alasql (str "INSERT INTO " table-name
                     " SELECT " (str/join ", " (get appdb/table-fields table-name))  " FROM CSV(?,{headers:true})") csv)))

(rf/reg-event-fx
 :load-new-db
 (fn [{:keys [db]} [_ table-csvs]]
   (js/console.log "Loading CSVs into DB sequentially...")
   (let [commands (mapv map-entry-to-command table-csvs)
         full-commands (-> commands
                           (conj #(js/console.log "All tables loaded and persisted to IndexedDB."))
                           (conj #(rf/dispatch [:update-last-updated]))
                           (conj #(rf/dispatch [:remove-old-dbs])))]
     (appdb/run-commands full-commands)
     {})))

(rf/reg-event-fx
 :remove-old-dbs
 (fn [{:keys [db]} _]
   (js/console.log "Removing old DBs...")
   (-> (js/window.indexedDB.databases)
       (.then (fn [dbs]
                (doseq [odb dbs]
                  (let [db-name (.-name odb)
                        current-name (str "metrajay_v" (:db-version db))]
                    (when (and
                           (re-find #"metrajay_v[0-9]+$" db-name)
                           (not= db-name current-name))
                      (js/console.log "Removing " db-name "...")
                      (js/window.indexedDB.deleteDatabase db-name)))))))
   {}))

(rf/reg-event-fx
 :load-all-stops
 (fn [{:keys [db]} [_ _]]
   {:alasql {:query "SELECT * FROM stops"
             :on-success [:set-available-stations]
             :on-failure [:bad-fetch-result]}}))

(rf/reg-event-fx
 :load-all-stops
 (fn [{:keys [db]} [_ _]]
   {:alasql {:query "SELECT * FROM stops"
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
 :update-db-version
 (fn [db [_ version]]
   (assoc db :db-version version)))
 
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
