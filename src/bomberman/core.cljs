(ns bomberman.core
  (:require [reagent.core :as reagent :refer [atom]]
            [figwheel.client :as fw :include-macros true]
            [jayq.core :refer [$]]
            [bomberman.views.menu :refer [menu]]
            [bomberman.views.game :refer [game]]
            [bomberman.views.highscores :refer [highscores]]
            [bomberman.views.settings :refer [settings]]))


(defn- create-route []
  (atom :menu))

(defn app []
  (let [route (create-route)]
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
