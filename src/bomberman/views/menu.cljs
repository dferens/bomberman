(ns bomberman.views.menu
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]))


(defn menu-view [app]
  (om/component
    (html
      [:div.menu-page
       [:img {:src "img/logo.jpg"}]
       [:ul
        (for [[link-route title] (:pages app)]
          [:li
           [:button
            {:on-click #(om/update! app :current-page link-route)}
            title]])]])))
