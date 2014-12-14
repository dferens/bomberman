(ns bomberman.core
  (:require [om.core :as om  :include-macros true]
            [sablono.core :refer-macros [html]]
            [figwheel.client :as fw :include-macros true]
            [jayq.core :refer [$]]
            [bomberman.views.menu]
            [bomberman.views.game]
            [bomberman.views.highscores]
            [bomberman.views.settings]))

(defn- create-app-state []
  {:current-page :menu
   :pages [[:game "New game"]
           [:highscores "Highscores"]
           [:settings "Settings"]]})


(defonce app-state (atom (create-app-state)))

(defn- start []
  (.each ($ ".bomberman-container")
    (fn [_ element]
      (om/root
        (fn [app owner]
          (om/component
            (html
              [:div.bomberman-game
               (om/build
                 (case (:current-page app)
                   :menu bomberman.views.menu/menu-view
                   :game bomberman.views.game/game-view
                   :settings bomberman.views.settings/settings-view
                   :highscores bomberman.views.highscores/highscores-view)
                 app)])))
        app-state
        {:target element}))))

(defn- stop []
  (.each
    ($ ".bomberman-container")
    (fn [_ element]
      (om/detach-root element))))

(fw/watch-and-reload :jsload-callback #(do (stop) (start)))
(start)