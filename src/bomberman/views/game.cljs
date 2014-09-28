(ns bomberman.views.game
  (:require [figwheel.client :as fw :include-macros true]))

(defn game []
  [:div.game-page
   [:div.window
    [:div.header
     [:span "Lives: 5"]
     [:span "Powerups: TODO"]]
    [:div.board
     [:p "Its a board"]]
    [:div.footer]]])
