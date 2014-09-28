(ns bomberman.views.menu)


(defn menu [{:keys [route pages]}]
  [:div.menu-page
    [:img {:src "img/logo.jpg"}]
      [:ul
       (for [[link-route title] pages]
         [:li
          [:button {:on-click #(reset! route link-route)} title]])]])
