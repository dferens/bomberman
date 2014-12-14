(ns bomberman.views.highscores
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]))


(defn highscores-view [app]
  (om/component
    (html
      [:div
       [:p "Highscores page"]])))
