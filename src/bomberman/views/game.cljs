(ns bomberman.views.game
  (:require [cljs.core.async :refer [<! timeout]]
            [jayq.util :refer [log]]
            [jayq.core :refer [$ bind]]
            [bomberman.world])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn- draw-row-cell [cell-atom]
  (let [cell @cell-atom
        css-class (case (:type cell)
                    :. "empty"
                    :obstacle "obstacle"
                    :boundary "boundary")]
    [:div.cell {:class css-class}]))

(defn- draw-board-row [row]
  [:div.board-row
   (for [cell-i (range (count row))]
     ^{:key cell-i} [draw-row-cell (nth row cell-i)])])

(defn- game-loop [world]
  (go
    (loop []
      (<! (timeout 100))
      (log "updated")
      (recur))))

(defn game []
  (let [world (bomberman.world/gen-world)]
    (game-loop world)
    (fn []
      (let [cells (-> world :map :cells)]
        [:div.game-page
         [:div.window
          [:div.header
           [:span "Lives: 5"]
           [:span "Powerups: TODO"]]
          [:div.board
           (for [cell-row-i (range (count cells))]
             ^{:key cell-row-i} [draw-board-row (nth cells cell-row-i)])]
          [:div.footer]]]))))

