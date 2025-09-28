(ns metrajay.app
  (:require [reagent.core :as r]
            [metrajay.events :as events]
            [metrajay.env]
            [reagent.dom.client :as rdomc]
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

(defn searchable-dropdown [subscription-keyword title set-station-event]
  (let [query (r/atom "")
        open? (r/atom false)]
    (fn []
      (let [available-stations (rf/subscribe [subscription-keyword])
            options (map :stop_name @available-stations)]
        [:div.mt-3        
         [:p title]
         [:p (first @available-stations)]
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
   [searchable-dropdown :available-stations-2 "Second Station" :set-station-2]])

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

(defn header []
  [:div.bg-red-500.flex.justify-between
   [:p.ml-2 "metrajay"]
   [:div
    [:p.mr-2 "updated"]]])

(defn root-component []
  [:div.flex.flex-col.w-full.max-w-lg.mx-auto
   [header]
   [:<>
    ;; [query-explorer]
    [search-page]
    
    
    ]])

(defonce root (delay (rdomc/create-root (.getElementById js/document "root"))))

(defn ^:export ^:dev/after-load init []
  (rf/dispatch-sync [:init-local-storage])
  (rf/dispatch-sync [:init-db]) 
  (rdomc/render @root [root-component]))
