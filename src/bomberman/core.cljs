(ns bomberman.core
  (:require [reagent.core :as reagent :refer [atom]]
            [figwheel.client :as fw :include-macros true]
            [jayq.core :refer [$]]
            [bomberman.views.menu]
            [bomberman.views.game]
            [bomberman.views.highscores]
            [bomberman.views.settings]))


(defn- create-route []
  (atom :menu))

(defn app []
  (let [pages [[:game "New game"]
               [:highscores "Highscores"]
               [:settings "Settings"]]
        route (create-route)]
    (fn []
      [:div.bomberman-game
       (case @route
         :menu [bomberman.views.menu/menu {:route route :pages pages}]
         :game [bomberman.views.game/game ]
         :highscores [bomberman.views.highscores/highscores]
         :settings [bomberman.views.settings/settings])])))

(defn start []
  (let [container-selector ".bomberman-container"]
    (.each ($ container-selector)
      #(reagent/render-component [app] %2))))

(defn- stop []
  (.each ($ ".bomberman-container")
    #(reagent/unmount-component-at-node %2)))

(start)
(fw/watch-and-reload :jsload-callback #(do (stop) (start)))
