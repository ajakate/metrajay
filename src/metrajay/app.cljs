(ns metrajay.app
  (:require [reagent.core :as r]
            [metrajay.events :as events]
            [reagent.dom.client :as rdomc]
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

(defn header []
  [:div.bg-red-500
   [:h1 "metrajay"]])

(defn root-component []
  [:div.flex.flex-col.w-full.max-w-lg.mx-auto
   [header]
   [simple-component]])

(defonce root (delay (rdomc/create-root (.getElementById js/document "root"))))

(defn ^:export ^:dev/after-load init []
  (rf/dispatch-sync [:init-local-storage])
  (rf/dispatch [:init-db])
  (rdomc/render @root [root-component]))
