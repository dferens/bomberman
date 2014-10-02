(ns bomberman.views.game
  (:require [cljs.core.async :refer [<! >! timeout chan put!]]
            [reagent.core :as reagent]
            [jayq.util :refer [log]]
            [jayq.core :refer [$ bind on]]
            [bomberman.world :as world]))

(def UNIT-SIZE-PX 50)

(defn- units->pixels [units]
  (* units UNIT-SIZE-PX))

(defn- board-cell-view [cell-atom]
  (let [cell @cell-atom
        css-class (case (:type cell)
                    :empty "empty"
                    :obstacle "obstacle"
                    :boundary "boundary")]
    [:div.cell {:class css-class}]))

(defn- board-row-view [row]
  [:div.board-row
   (for [cell-i (range (count row))]
     ^{:key cell-i} [board-cell-view (nth row cell-i)])])

(defn- stats-view [world]
  (let [player @(:player world)]
    [:div.header
     [:div.lives "Lives: " (:lives player)]
     [:div.powerups
      [:p.speed "Speed: " (-> player :powerups :speed)]]]))

(defn- player-view [player-atom]
  (let [player @player-atom
        [x y] (map units->pixels (:pos player))]
    [:div.player {:style {:top y :left x
                          :width (-> player :width units->pixels)
                          :height (-> player :height units->pixels)}}]))

(defn- setup-bindings [input-chan]
  (let [button-key-codes {37 :left 39 :right
                          38 :top 40 :bottom}]
    (on ($ "body") :keydown
      (fn [event]
        (let [key-code (.-keyCode event)
              direction (get button-key-codes key-code)]
          (when direction
            (put! input-chan direction)))))))

(defn game []
  (let [world (bomberman.world/create)
        input-chan (chan)]
    (setup-bindings input-chan)
    (bomberman.world/run-game-loop! world input-chan)
    (fn []
      (let [cells (-> world :game-map :cells)]
        [:div.game-page
         [:div.window
          [stats-view world]
          [:div.board
           [player-view (:player world)]
           (for [cell-row-i (range (count cells))]
             ^{:key cell-row-i} [board-row-view (nth cells cell-row-i)])]
          [:div.footer]]]))))
