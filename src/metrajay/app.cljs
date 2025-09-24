(ns metrajay.app
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]))

(defn simple-component []
  [:div
   [:p.text-5xl "I am nothing"]
   [:p.someclass
    "I have " [:strong "bold"]
    [:span {:style {:color "red"}} " and red "] "text."]])

(defn ^:dev/after-load init []
  (rdom/render [simple-component] (js/document.getElementById "root")))
