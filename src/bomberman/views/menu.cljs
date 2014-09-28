(ns bomberman.views.menu)


(def links
  [[:game "New game"]
   [:highscores "Highscores"]
   [:settings "Settings"]])

(defn menu [{:keys [route]}]
  [:div.menu-page
    [:img {:src "img/logo.jpg"}]
      [:ul
       (for [[link-route title] links]
         [:li
          [:button {:on-click #(reset! route link-route)} title]])]])
