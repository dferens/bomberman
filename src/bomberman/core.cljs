(ns bomberman.core
  (:require [reagent.core :as reagent :refer [atom]]
            [figwheel.client :as fw :include-macros true]))


(def route (atom :menu))

(defn menu []
  [:div.menu
   [:table
    [:tbody
     [:tr.row1
      [:td.col1]
      [:td.col2
       [:img {:src "img/logo.jpg"}]]
      [:td.col3]]
     [:tr.row2
      [:td.col1]
      [:td.col2
       [:div.links
        [:ul
         [:li [:button "New game"]]
         [:li [:button "Highscores"]]
         [:li [:button "Settings"]]]]]
      [:td.col3]]
     ]]])

(defn game []
  [:div
   [:p "Its a game"]
   [:button {:on-click #(reset! route :menu)} "To menu"]])

(defn app [props]
  [:div.bomberman-game
   (case @route
      :menu [menu]
      :game [game])])

(fw/watch-and-reload)
(reagent/render-component [app] (.-body js/document))
