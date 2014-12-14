(ns bomberman.views.game
  (:require [om.core :as om :include-macros true]
            [sablono.core :refer-macros [html]]
            [cljs.core.async :refer [<! >! timeout chan put!]]
            [jayq.util :refer [log]]
            [jayq.core :refer [$ on off]]
            [bomberman.world :as world])
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
(def place-bomb-button :space)

(defn- units->pixels [units]
  (* units pixels-per-unit))


;; Views

(defn- player-view [{pos :pos}]
  (om/component
    (let [[x y] (map units->pixels pos)
         [width height] (map units->pixels [1 1])]
      (html
        [:div.player
         {:style {:top y
                  :left x
                  :width width
                  :height height}}]))))

(defn- creep-view [{pos :pos}]
  (om/component
    (let [[x y] (map units->pixels pos)
          [width height] (map units->pixels [1 1])]
      (html
        [:div.creep
         {:style {:top y
                  :left x
                  :width width
                  :height height}}]))))

(defn- row-view [row]
  (om/component
    (html
      [:div.board-row
       (for [cell row
             :let [cell-type (or (:type cell) :empty)]]
         [:div.cell {:class (name cell-type)}])])))

(defn- stats-view [player]
  (om/component
    (html
      [:div.header
       [:div.lives "Lives: " (get-in player [:powerups :lives])]
       [:div.powerups]])))

(defn game-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:world (bomberman.world/create)
       :buttons-stack (sorted-set)})

    om/IWillMount
    (will-mount [this]
      (go
        (loop [_ (<! (timeout 20))]
          (let [pressed-buttons (om/get-state owner :buttons-stack)]
            (if-not (empty? pressed-buttons)
              (om/update-state! owner :world
                #(world/move-player % (last pressed-buttons)))))
          (om/update-state! owner :world #(world/step % 20))
          (recur (<! (timeout 20)))))

      (on ($ "body") :keydown
          (fn [event]
            (let [button (keycodes (.-keyCode event))]
              (if-not (nil? button)
                (cond
                  (contains? move-buttons button)
                  (om/update-state! owner :buttons-stack #(conj % button))

                  (= button place-bomb-button)
                  (om/update-state! owner :world world/place-bomb))))))

      (on ($ "body") :keyup
          (fn [event]
            (let [button (keycodes (.-keyCode event))
                  pressed-buttons (om/get-state owner :buttons-stack)]
              (if (contains? pressed-buttons button)
                (om/update-state! owner :buttons-stack #(disj % button)))))))

    om/IWillUnmount
    (will-unmount [this]
      (off ($ "body") :keydown)
      (off ($ "body") :keyup))

    om/IRenderState
    (render-state [this {world :world}]
      (html
        [:div.game-page
         [:div.window
          (om/build stats-view (:player world))
          [:div.board
           (om/build player-view (:player world))
           (om/build-all creep-view (:creeps world))
           (om/build-all row-view (:cells world))]]]))))