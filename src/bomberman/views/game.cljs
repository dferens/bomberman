(ns bomberman.views.game
  (:require [cljs.core.async :refer [<! >! timeout chan put!]]
            [reagent.core :as reagent]
            [jayq.util :refer [log]]
            [jayq.core :refer [$ bind on]]
            [bomberman.world :as world])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def CELL-SIZE-PX 50)

(def BUTTONS
  {37 :left
   38 :top
   39 :right
   40 :bottom})

(def POS-DELTA
  {:left [-1 0]
   :top [0 -1]
   :right [1 0]
   :bottom [0 1]})

(defn- axis->pixels [axis-value]
  (* axis-value CELL-SIZE-PX))

(defn- board-cell-view [cell-atom]
  (let [cell @cell-atom
        css-class (case (:type cell)
                    :. "empty"
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
        [x y] (map axis->pixels (:pos player))]
    [:div.player {:style {:top y :left x}}]))

(defn- setup-bindings [input-chan]
  (on ($ "body") :keydown
    (fn [event]
      (let [key-code (.-keyCode event)]
        (if (contains? BUTTONS key-code)
          (put! input-chan (get BUTTONS key-code)))))))

(defn- setup-game-loop [world input-chan]
  (letfn []
    (go
      (loop [direction (<! input-chan)]
        (let [delta-vec (get POS-DELTA direction)]
          (world/move-player! world delta-vec)
          (recur (<! input-chan)))))))

(defn game []
  (let [world (bomberman.world/create)
        input-chan (chan)]
    (setup-bindings input-chan)
    (setup-game-loop world input-chan)
    (fn []
      (let [cells (-> world :map :cells)]
        [:div.game-page
         [:div.window
          [stats-view world]
          [:div.board
           [player-view (:player world)]
           (for [cell-row-i (range (count cells))]
             ^{:key cell-row-i} [board-row-view (nth cells cell-row-i)])]
          [:div.footer]]]))))
