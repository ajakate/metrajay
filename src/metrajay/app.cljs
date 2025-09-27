(ns metrajay.app
  (:require [reagent.core :as r]
            [metrajay.events :as events]
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

(defn search-component []
  [:div.flex-flex-col.mx-3
   [:div.nes-field
    [:label "Select first station"]
    [:input.nes-input {:type "text" :list "stations"}]
    [:datalist {:id "stations"}
     [:option "Station 1"]
     [:option "Station 2"]]]
   [:div.nes-field
    [:label "Select second station"]
    [:input.nes-input {:type "text" :disabled true}]]])

(defn searchable-dropdown []
  (let [query (r/atom "")
        open? (r/atom false)
        options ["Union Station" "Ridgewood" "Oak Park" "Downers Grove" "Halstead" "Geneva"]]
    (fn []
      [:div.nes-container.with-title.is-centered.mt-3.mx-3
       [:p.title "Search Route"]

       ;; Input
       [:p "First Station"]
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
                          (reset! open? false))}
             opt])])
       

       [:p "more text"]
       [:p "more text"]
       [:p "more text"]
       [:p "more text"]
       [:p "more text"]
       
       
       
       ])))

(defn header []
  [:div.bg-red-500.flex.justify-between
   [:p.ml-2 "metrajay"]
   [:div
    [:p.mr-2 "updated"]]])

(defn root-component []
  [:div.flex.flex-col.w-full.max-w-lg.mx-auto
   [header]
   [searchable-dropdown]])

(defonce root (delay (rdomc/create-root (.getElementById js/document "root"))))

(defn ^:export ^:dev/after-load init []
  (rf/dispatch-sync [:init-local-storage])
  (rf/dispatch [:init-db])
  (rdomc/render @root [root-component]))
