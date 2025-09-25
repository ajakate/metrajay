(ns metrajay.events
  (:require
   ["jszip" :as JSZip]
   [re-frame.core :as rf]
   [superstructor.re-frame.fetch-fx]
   [metrajay.db :as appdb]
   [clojure.string :as str]
   [akiroz.re-frame.storage :refer [persist-db-keys]]))


(defn persisted-reg-event-db
  [event-id handler]
  (rf/reg-event-fx
   event-id
   [(persist-db-keys :metrajay-app [:last_updated :db_version])]
   (fn [{:keys [db]} event-vec]
     {:db (handler db event-vec)})))

(persisted-reg-event-db :init-local-storage (fn [db] db))

(rf/reg-event-fx
 :init-db
 (fn [{:keys [db]} [_ _]]
   (let [raw-version (:db_version db)
         version (if (nil? raw-version) 0 raw-version)
         reset-commands (appdb/db-reset-commands version)
         full-commands (conj reset-commands #(rf/dispatch [:update-db-version version]))]
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
            :on-success [:set-update]
            :on-failure [:bad-fetch-result]}}))

(rf/reg-event-fx
 :download-schedule
 (fn [{:keys [db]} event-vec]
   {:fetch {:method :get
            :url "http://localhost:8787?route=schedule"
            :mode :cors
            :credentials :omit
            :response-content-types {#"application/zip" :array-buffer}
            :on-success [:zip-success]}}))

(rf/reg-event-db
 :set-update
 (fn [db [_ {:keys [body]}]]
   (assoc db :update body)))

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
   (let [new-db-version (inc (:db_version db))
         commands (appdb/db-reset-commands new-db-version)
         full-commands (-> commands
                           (conj #(rf/dispatch [:update-db-version new-db-version]))
                           (conj #(rf/dispatch [:load-new-db table-csvs])))]
     (appdb/run-commands full-commands)
     {:db (assoc db :db_version new-db-version :debug table-csvs)})))


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
                        current-name (str "metrajay_v" (:db_version db))]
                    (when (and
                           (re-find #"metrajay_v[0-9]+$" db-name)
                           (not= db-name current-name))
                      (js/console.log "Removing " db-name "...")
                      (js/window.indexedDB.deleteDatabase db-name)))))))

   {}))

(rf/reg-event-fx
 :bad-fetch-result
 (fn [{:keys [db]} [_ error]]
   {:db (assoc db :error error)}))

(persisted-reg-event-db
 :update-db-version
 (fn [db [_ version]]
   (assoc db :db_version version)))
