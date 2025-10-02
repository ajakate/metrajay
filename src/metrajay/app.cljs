(ns metrajay.app
  (:require [reagent.core :as r]
            [metrajay.events :as events]
            [metrajay.env]
            [reagent.dom.client :as rdomc]
            [metrajay.util :as u]
            [clojure.string :as str]
            [reitit.frontend :as reitit]
            [reitit.frontend.easy :as rfe]
            [re-frame.core :as rf]))

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

(defn home-page []
  (fn []
    (let [favorites @(rf/subscribe [:favorites])]
      [:div.nes-container.with-title.is-centered.mt-3.mx-3
       [:p.title "Saved Routes"]
       (if (seq favorites)
         [:<>
          [:table.nes-table.is-bordered 
           [:tbody
            (for [i favorites]
              ^{:key (str i)}
              [:tr
               [:td 
                [:button.nes-btn
                 {:on-click #(rfe/push-state :schedule {} {:stop1 (-> i first :stop_id) :stop2 (-> i second :stop_id)})}
                 (str (-> i first :stop_id) "-" (-> i second :stop_id))]] 
               [:td
                [:button.nes-btn
                 {:on-click #(rf/dispatch [:remove-from-favorites i])}
                 "Delete"]]]
              )]]]
         [:<>
          [:p.mb-3 "You currently have no saved routes..."]
          [:button.nes-btn
           {:on-click #(rfe/push-state :search)}
           [:div
            [:span "Search Routes"]
            [:i.nes-icon.search]]]]
         )])))

(defn searchable-dropdown [subscription-keyword title set-station-event station-sub query-event query-sub]
  (let [open? (r/atom false)]
    (fn []
      (let [available-stations (rf/subscribe [subscription-keyword])
            selected-station (rf/subscribe [station-sub])
            query (rf/subscribe [query-sub])
            options @available-stations]
        [:div.mt-3
         [:p title]
         [:input.nes-input.w-full
          {:type "text"
           :placeholder "Search..."
           :value (if @selected-station (:stop_name @selected-station) @query)
           :on-focus #(reset! open? true)
           :on-change #(rf/dispatch [query-event (.. % -target -value)])
           :disabled (not= @selected-station nil)}]

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
   [:button.nes-btn.is-primary
    {:on-click #(rf/dispatch [:clear-search-selection])}
    "Clear Selection"]

   [searchable-dropdown :available-stations "First Station" :set-station-1 :station-1 :set-query-1 :query-1]
   [searchable-dropdown :available-stations-2 "Second Station" :set-station-2 :station-2 :set-query-2 :query-2]
   (let [can-submit-search (rf/subscribe [:can-submit-search])]
     [:button.nes-btn.mt-3
      {:on-click #(rf/dispatch [:navigate-to-schedule]) :class (if @can-submit-search "is-primary" "is-disabled")}
      "Get Schedule"])])

(defn schedule-page []
  (let [schedule (rf/subscribe [:schedule])
        stations [(:station1 @schedule) (:station2 @schedule)]
        can-add-to-favorites @(rf/subscribe [:can-add-to-favorites])]
    (r/with-let [active-bound (r/atom :inbound)
                 active-key   (r/atom nil)]
      (let [station-1 (-> stations first :stop_name)
            station-2 (-> stations second :stop_name)
            _ (when (and (nil? @active-key) (seq @schedule))
                (reset! active-key (-> @schedule (get @active-bound) keys first)))
            schedule-list (get-in @schedule [@active-bound @active-key])]
        [:<>
         [:div.flex.flex-col.nes-container.p-1
          [:div
           [:button.nes-btn
            {:on-click #(rf/dispatch [:add-to-favorites stations])}
            (if can-add-to-favorites "Add to Favorites" "nope")]]
          [:div.flex.flex-row
           [:button.nes-btn.grow {:class (when (= @active-bound :inbound) "is-primary is-disabled")
                                  :on-click #(reset! active-bound :inbound)}
            "inbound"]
           [:button.nes-btn.grow {:class (when (= @active-bound :outbound) "is-primary is-disabled")
                                  :on-click #(reset! active-bound :outbound)}
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
            [:th (if (= @active-bound :inbound) station-1 station-2)]
            [:th (if (= @active-bound :inbound) station-2 station-1)]]]
          [:tbody
           (for [i schedule-list]
             ^{:key (str (:time1 i) "-" (:time2 i))}
             [:tr
              [:td (u/format-time (if (= @active-bound :inbound) (:time1 i) (:time2 i)))]
              [:td (u/format-time (if (= @active-bound :inbound) (:time2 i) (:time1 i)))]])]]]))))

(defn header []
  [:div.bg-red-500.flex.justify-between
   [:p.ml-2 "metrajay"] 
   [:button
    {:on-click #(rfe/push-state :home)}
    "Home"
    ]])

(defn root-component []
  [:div.flex.flex-col.w-full.max-w-lg.mx-auto
   [header]
   (let [view @(rf/subscribe [:current-view])]
     (when view
       [view]))])

(def routes
  [["/"
    {:name :home
     :view home-page
     :controllers
     [{:start (fn [_] (js/console.log "Enter home"))
       :stop  (fn [_] (js/console.log "Leaving home"))}]}]
   ["/search"
    {:name :search
     :view search-page
     :controllers
     [{:start (fn [_] (rf/dispatch [:clear-search-selection]))
       :stop  (fn [_] (js/console.log "Leaving search"))}]}]

   ["/schedule"
    {:name :schedule
     :view schedule-page
     :controllers
     [{:parameters {:query [:stop1 :stop2]}   ;; make `:id` available
       :start (fn [{:keys [query]}]
                (rf/dispatch [:get-schedule [(:stop1 query) (:stop2 query)]]))
       :stop (fn [_]
               (js/console.log "TODO: clear schedule maybe"))}]}]])

(def router (reitit/router routes))

(defn init-routes! []
  (rfe/start!
   router
   (fn [match]
     ;; called whenever URL changes
     (rf/dispatch [:navigated match]))
   {:use-fragment true}))

(defonce root (delay (rdomc/create-root (.getElementById js/document "root"))))

(defn ^:export ^:dev/after-load init []
  (rf/dispatch-sync [:init-local-storage])
  (rf/dispatch-sync [:init-db])
  (init-routes!)
  (rdomc/render @root [root-component])
  ;;   (rdomc/render @root [query-explorer])
  )
