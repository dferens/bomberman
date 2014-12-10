(ns bomberman.views.game
  (:require [cljs.core.async :refer [<! >! timeout chan put!]]
            [reagent.core :as reagent]
            [jayq.util :refer [log]]
            [jayq.core :refer [$ bind on find]]
            [bomberman.game])
  (:require-macros [cljs.core.async.macros :refer [go]]))


;; Constants

(def pixels-per-unit 50)

(def keycodes
  "Maps concrete keyboard buttons to abstract buttons"
  {37 :left
   39 :right
   38 :top
   40 :bottom
   32 :space})

(def move-buttons #{:left :right :top :bottom})

(defn- units->pixels [units]
  (* units pixels-per-unit))


;; Views

(defn- player-view [world-atom]
  (let [{:keys [pos size]} (:player @world-atom)
        [x y] (map units->pixels pos)
        [width height] (map units->pixels size)]
    [:div.player
     {:style {:top y
              :left x
              :width width
              :height height}}]))

(defn- board-view [world-atom]
  (let [gamemap (:gamemap @world-atom)]
    [:div.board
     [player-view world-atom]
     (for [row-i (range (count gamemap))]
       ^{:key row-i}
       [:div.board-row
        (for [{:keys [x type]} (nth gamemap row-i)]
          ^{:key x} [:div.cell {:class (name type)}])])]))

(defn- stats-view [world-atom]
  (let [player (:player @world-atom)]
    [:div.header
     [:div.lives "Lives: " (get-in player [:powerups :lives])]
     [:div.powerups]]))


(defn- init-game []
  (let [delta-time-ms 20
        game (bomberman.game/create)
        world-atom (reagent/atom nil)
        move-buttons-stack (atom (sorted-set))]

    ; Process responses
    (go
      (loop [response (<! (:responses-chan game))]
        (if (= (:topic response) :world-update)
          (reset! world-atom (:world-state response)))
        (recur (<! (:responses-chan game)))))

    ; Setup world steps
    (go
      (loop [_ (<! (timeout delta-time-ms))]
        (if-not (empty? @move-buttons-stack)
          (bomberman.game/move! game (last @move-buttons-stack)))
        (bomberman.game/step! game delta-time-ms)
        (recur (<! (timeout delta-time-ms)))))

    ; Setup bindings
    (on ($ "body") :keydown
        (fn [event]
          (let [button (keycodes (.-keyCode event))]
            (if-not (nil? button)
              (cond
                (contains? move-buttons button) (swap! move-buttons-stack conj button)
                :else (log (str "Key pressed:" (.-keyCode event))))))))
    (on ($ "body") :keyup
        (fn [event]
          (let [button (keycodes (.-keyCode event))]
            (if (contains? @move-buttons-stack button)
              (swap! move-buttons-stack disj button)))))

    ; Send init command
    (bomberman.game/init! game)

    [game world-atom]))

(defn game []
  (let [[_ world-atom] (init-game)]
    (fn []
      [:div.game-page
       [:div.window
        [stats-view world-atom]
        [board-view world-atom]
        [:div.footer]]])))