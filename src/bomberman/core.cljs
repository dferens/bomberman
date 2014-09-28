(ns bomberman.core
  (:require [reagent.core :as reagent :refer [atom]]
            [figwheel.client :as fw :include-macros true]))


(def route (atom :menu))

(defn- switch-page [to]
  (reset! route to))

(defn menu []
  [:div.menu-page
   [:table
    [:tbody
     [:tr
      [:td.col1]
      [:td.col2
       [:div.menu
        [:img {:src "img/logo.jpg"}]
        [:ul
         (for [[route title] [[:game "New game"]
                              [:highscores "Highscores"]
                              [:settings "Settings"]]]
           [:li
            [:button {:on-click #(switch-page route)} title]])
         ]]]
      [:td.col3]]
     ]]])

(defn game []
  [:div
   [:p "Its a game"]
   [:button {:on-click #(switch-page :menu)} "To menu"]])

(defn highscores []
  [:div
   [:p "Highscores page"]])

(defn settings []
  [:div
   [:p "Settings page"]])

(defn app [props]
  [:div.bomberman-game
   (case @route
      :menu [menu]
      :game [game]
      :highscores [highscores]
      :settings [settings])])

(fw/watch-and-reload)
(reagent/render-component [app] (.-body js/document))
