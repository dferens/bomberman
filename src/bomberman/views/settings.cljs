(ns bomberman.views.settings
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]))


(defn settings-view [app]
  (om/component
    (html
      [:div
       [:p "Settings page"]])))
