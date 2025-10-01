(ns metrajay.app
  (:require [reagent.core :as r]
            [metrajay.events :as events]
            [metrajay.env]
            [reagent.dom.client :as rdomc]
            [metrajay.util :as u]
            [clojure.string :as str]
            [re-frame.core :as rf]))

(defn simple-component []
  [:div
   [:p.text-5xl "I am nothing"]
   [:p.someclass
    "I have " [:strong "bold"]
    [:span {:style {:color "red"}} " and red "] "text."]
   [:button.nes-btn.is-primary {:type "button" :on-click #(rf/dispatch [:check-for-update])} "update"]
   [:br]
   [:button.nes-btn.is-warning {:on-click #(rf/dispatch [:download-schedule])} "download schedule"]])

(defn query-explorer []
  (let [query (r/atom "select * from stops limit 10")] ; start with default SQL
    (fn []
      (let [sample-query (rf/subscribe [:sample-query])]
        [:div.flex.flex-col.space-y-4

         ;; Controls
         [:div.flex.flex-col.space-y-2
          [:textarea.nes-textarea
           {:value @query
            :rows 5
            :on-change #(reset! query (.. % -target -value))}]
          [:button.nes-btn.is-primary
           {:on-click #(rf/dispatch [:run-sample-query @query])}
           "Run"]]

         ;; Results
         [:div
          [:p "Results:"]
          (when (seq @sample-query)
            [:table.nes-table.is-bordered.is-centered.w-full.overflow-x-auto
             [:thead
              [:tr
               (for [col (keys (first @sample-query))]
                 ^{:key col}
                 [:th (name col)])]]
             [:tbody
              (for [row @sample-query]
                ^{:key (hash row)}
                [:tr
                 (for [col (keys row)]
                   ^{:key col}
                   [:td (str (get row col))])])]])]]))))

(defn searchable-dropdown [subscription-keyword title set-station-event]
  (let [query (r/atom "")
        open? (r/atom false)]
    (fn []
      (let [available-stations (rf/subscribe [subscription-keyword])
            options @available-stations]
        [:div.mt-3        
         [:p title]
         [:input.nes-input.w-full
          {:type "text"
           :placeholder "Search..."
           :value @query
           :on-focus #(reset! open? true)
           :on-change #(reset! query (.. % -target -value))}]
         
         ;; Dropdown
         (when @open?
           [:div#dropdown.nes-list.is-disc.bg-white.max-h-100.overflow-y-auto.mt-2.border.border-gray-300
            (for [opt (filter #(str/includes?
                                (.toLowerCase %)
                                (.toLowerCase @query))
                              options)]
              ^{:key opt}
              [:button.nes-btn.w-full.text-left
               {:on-click (fn []
                            (reset! query opt)
                            (reset! open? false)
                            (rf/dispatch [set-station-event opt]))}
               opt])])]))))

(defn search-page []
  [:div.nes-container.with-title.is-centered.mt-3.mx-3
   [:p.title "Search Route"]
   [:button.nes-btn.is-primary
{:on-click #(rf/dispatch [:download-schedule])}
    "Schedule"]
   [:button.nes-btn.is-primary
    {:on-click #(rf/dispatch [:load-all-stops])}
    "Search"]

   [searchable-dropdown :available-stations "First Station" :set-station-1]
   [searchable-dropdown :available-stations-2 "Second Station" :set-station-2]
   (let [can-submit-search (rf/subscribe [:can-submit-search])]
     [:button.nes-btn.mt-3
      {:on-click #(rf/dispatch [:format-schedule-stations]) :class (if @can-submit-search "is-primary" "is-disabled")}
      "Get Schedule"])])

(defn schedule-page []
  (let [schedule (rf/subscribe [:schedule])
        stations (rf/subscribe [:schedule-stations])]
    (r/with-let [active-bound (r/atom "inbound")
                 active-key   (r/atom nil)]
      (let [station-1 (-> @stations first :stop_name)
            station-2 (-> @stations second :stop_name)
            _ (when (and (nil? @active-key) (seq @schedule))
                (reset! active-key (-> @schedule (get @active-bound) keys first)))
            schedule-list (get-in @schedule [@active-bound @active-key])]
        [:<>
         [:div.flex.flex-col.nes-container.p-1
          [:div.flex.flex-row
           [:button.nes-btn.grow {:class (when (= @active-bound "inbound") "is-primary is-disabled")
                             :on-click #(reset! active-bound "inbound")}
            "inbound"]
           [:button.nes-btn.grow {:class (when (= @active-bound "outbound") "is-primary is-disabled")
                             :on-click #(reset! active-bound "outbound")}
            "outbound"]]
          [:div.flex.flex-row
           (for [k (keys (get @schedule @active-bound))]
             ^{:key k}
             [:button.nes-btn.grow
              {:on-click #(reset! active-key k)
               :class (when (= @active-key k) "is-primary is-disabled")}
              k])]]
         [:table.nes-table.is-bordered
          [:thead
           [:tr
            [:th (if (= @active-bound "inbound") station-1 station-2)]
            [:th (if (= @active-bound "inbound") station-2 station-1)]]]
          [:tbody
           (for [i schedule-list]
             ^{:key (str (:time1 i) "-" (:time2 i))}
             [:tr
              [:td (u/format-time (if (= @active-bound "inbound") (:time1 i) (:time2 i)))]
              [:td (u/format-time (if (= @active-bound "inbound") (:time2 i) (:time1 i)))]])]]]))))

(defn header []
  [:div.bg-red-500.flex.justify-between
   [:p.ml-2 "metrajay"]
   [:div
    [:p.mr-2 "updated"]]])

(defn root-component []
  [:div.flex.flex-col.w-full.max-w-lg.mx-auto
   [header]
   [:<>
    ;; [search-page]
    [schedule-page]
    
    
    ]])

(defonce root (delay (rdomc/create-root (.getElementById js/document "root"))))

(defn ^:export ^:dev/after-load init []
  (rf/dispatch-sync [:init-local-storage])
  (rf/dispatch-sync [:init-db]) 
  (rdomc/render @root [root-component])
;;   (rdomc/render @root [query-explorer])
  )
