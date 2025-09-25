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
 (fn [_ [_ _]]
   (appdb/init-db)
   {}))


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
     (js/console.log "Unzipping..." response)
     (js/console.log "Unzipping..." body)
     (-> (.loadAsync zip body)
         (.then (fn [unzipped]
                  (js/Promise.all
                   (for [[filename _] appdb/table-map]
                     (.. unzipped (file filename) (async "string"))))))
         (.then (fn [texts]
                  (rf/dispatch [:load-csvs
                                (map vector (vals appdb/table-map) texts)])))))
   {}))

(rf/reg-event-fx
 :load-csvs
 (fn [_ [_ table-csvs]]
   (js/console.log "Loading CSVs into DB sequentially...")
   (let [chain
         (reduce (fn [prev [table-name csv]]
                   (.then prev
                          (fn [_]
                            (-> (.promise js/alasql (str "DELETE FROM " table-name " WHERE 1"))
                                (.then (fn [_] (.promise js/alasql (str "INSERT INTO " table-name
                                                                        " SELECT " (str/join ", " (get appdb/table-fields table-name))  " FROM CSV(?,{headers:true})") csv)))))))
                 (.resolve js/Promise)
                 table-csvs)]
     (.then chain
            (fn [_]
              (js/console.log "All tables loaded and persisted to IndexedDB."))))
   {}))


(rf/reg-event-fx
 :bad-fetch-result
 (fn [{:keys [db]} [_ error]]
   {:db (assoc db :error error)}))
