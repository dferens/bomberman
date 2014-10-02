(ns bomberman.views.game
  (:require [cljs.core.async :refer [<! >! timeout chan put!]]
            [reagent.core :as reagent]
            [jayq.util :refer [log]]
            [jayq.core :refer [$ bind on find focus]]
            [bomberman.world :as world]))

(def UNIT-SIZE-PX 50)

(defn- units->pixels [units]
  (* units UNIT-SIZE-PX))

(defn- board-cell-view [cell]
  (let [css-class (case (:type cell)
                    :empty "empty"
                    :obstacle "obstacle"
                    :boundary "boundary")]
    [:div.cell {:class css-class}]))

(defn- board-row-view [row]
  [:div.board-row
   (for [cell-i (range (count row))]
     ^{:key cell-i} [board-cell-view (nth row cell-i)])])

(defn- stats-view [world-atom]
  (let [player (:player @world-atom)]
    [:div.header
     [:div.lives "Lives: " (:lives player)]
     [:div.powerups
      [:p.speed "Speed: " (-> player :powerups :speed)]]]))

(defn- player-view [{:keys [player]}]
  (let [[x y] (map units->pixels (:pos player))]
    [:div.player {:style {:top y :left x
                          :width (-> player :width units->pixels)
                          :height (-> player :height units->pixels)}}]))

(defn- keyboard-buttons-collector
  [input-chan]
  (let [button-key-codes {37 :left 39 :right 38 :top 40 :bottom}]
    [:input.keyboard-input
     {:type :text
      :style {:opacity 0 :position :absolute :top "-9999px"}
      :on-key-down (fn [event]
                    (let [key-code (.-keyCode event)
                          direction (get button-key-codes key-code)]
                      (when direction (put! input-chan direction))))}]))

(defn game []
  (let [world-atom (bomberman.world/create)
        input-chan (chan)]
    (bomberman.world/run-game-loop! world-atom input-chan)
    (fn []
      (let [cells (-> @world-atom :game-map :cells)]
        [:div.game-page
         {:on-click #(-> ($ (.-currentTarget %))
                         (find :.keyboard-input)
                         (.focus))}
         [:div.window
          [keyboard-buttons-collector input-chan]
          [stats-view world-atom]
          [:div.board
           [player-view {:player (:player @world-atom)}]
           (for [cell-row-i (range (count cells))]
             ^{:key cell-row-i} [board-row-view (nth cells cell-row-i)])]
          [:div.footer]]]))))
