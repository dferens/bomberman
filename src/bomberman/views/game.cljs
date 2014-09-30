(ns bomberman.views.game
  (:require [bomberman.world]))

(enable-console-print!)

(defn- cell-block-class [cell]
  (case (:type cell)
    :. "empty"
    :boundary "boundary"
    :obstacle "obstacle"
    nil "nil" ))

(defn- draw-cell [{:keys [cell]}]
  [:div.cell {:class (cell-block-class cell)}])

(defn- draw-board-row [{:keys [row]}]
  [:div.board-row
   (for [cell row]
     (draw-cell {:cell cell}))])

(defn game []
  (let [world (bomberman.world/gen-world)]
    [:div.game-page
     [:div.window
      [:div.header
       [:span "Lives: 5"]
       [:span "Powerups: TODO"]]
      [:div.board
       (for [cell-row (-> world :map :cells)]
         (draw-board-row {:row cell-row}))]
      [:div.footer]]]))
