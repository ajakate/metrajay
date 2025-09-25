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
   [:button {:on-click #(rf/dispatch [:check-for-update])} "update"]
   [:br]
   [:button {:on-click #(rf/dispatch [:download-schedule])} "download schedule"]])

(defonce root (delay (rdomc/create-root (.getElementById js/document "root"))))

(defn ^:export ^:dev/after-load init []
  (rf/dispatch [:init-local-storage])
  (rf/dispatch [:init-db])
  (rdomc/render @root [simple-component]))
