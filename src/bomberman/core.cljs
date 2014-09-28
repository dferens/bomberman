(ns bomberman.core
  (:require [reagent.core :as reagent :refer [atom]]
            [figwheel.client :as fw :include-macros true]
            [jayq.core :refer [$]]))


(defn menu [{:keys [route]}]
  [:div.menu-page
   [:table
    [:tbody
     [:tr
      [:td.col1]
      [:td.col2
       [:div.menu
        [:img {:src "img/logo.jpg"}]
        [:ul
         (for [[link-route title] [[:game "New game"]
                                   [:highscores "Highscores"]
                                   [:settings "Settings"]]]
           [:li
            [:button {:on-click #(reset! route link-route)} title]])
         ]]]
      [:td.col3]]]]])

(defn game []
  [:div
   [:p "Its a game"]
   [:button "To menu"]])

(defn highscores []
  [:div
   [:p "Highscores page"]])

(defn settings []
  [:div
   [:p "Settings page"]])

(defn app []
  (let [route (atom :menu)]
    (fn []
      [:div.bomberman-game
       (case @route
         :menu [menu {:route route}]
         :game [game]
         :highscores [highscores]
         :settings [settings])])))

(defn run []
  (.each ($ ".bomberman-container")
    #(reagent/render-component [app] %2)))

(run)
(fw/watch-and-reload)
