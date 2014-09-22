(ns bomberman.core
  (:require [reagent.core :as reagent :refer [atom]]
            [figwheel.client :as fw :include-macros true]))


(def route (atom :menu))


(defn menu []
  [:div
   [:p "Its a menu"]
   [:button {:on-click #(reset! route :game)} "To game"]])

(defn game []
  [:div
   [:p "Its a game"]
   [:button {:on-click #(reset! route :menu)} "To menu"]])

(defn routes [props]
  (case @route
    :menu [menu]
    :game [game]))

(fw/watch-and-reload)
(reagent/render-component [routes] (.-body js/document))
