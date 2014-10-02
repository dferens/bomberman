(ns bomberman.world
  (:require [cljs.core.async :refer []]
            [reagent.core :as reagent :refer [atom]])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(def DIRECTIONS
  {:left [-1 0] :right [1 0]
   :top [0 -1] :bottom [0 1]})

(defn get-next-pos
  ([pos direction delta-time]
   (next-pos pos direction delta-time 1))
  ([pos direction delta-time speed]
    (map #(+ %1 (* %2 delta-time speed)) pos (get DIRECTIONS direction))))

(defprotocol MapProtocol
  (get-cell-at [this pos])
  (get-cells-inside [this bouding-box]))

(defprotocol PlayerProtocol
  (get-bounding-box [this])
  (move [this direction delta-time]))

(defrecord Rect [x y width height])

(defrecord Cell [type is-obstacle])

(defrecord Map [cells]
  MapProtocol
  (get-cell-at
    [this pos]
    (let [[col-i row-i] (map int pos)]
      (-> cells (nth row-i) (nth col-i))))
  (get-cells-inside
    [this {:keys [x y width height]}]
    (let [start-x (int x)
          start-y (int y)
          end-x (int (+ x width 1))
          end-y (int (+ y height 1))]
      (for [x (range start-x end-x)
            y (range start-y end-y)]
        (get-cell-at this [x y])))))

(defrecord Player [pos lives powerups width height]
  PlayerProtocol
  (get-bounding-box
    [this]
    {:x (first pos) :y (second pos)
     :width width :height height})
  (move
    [this direction delta-time]
    (let [speed (-> this :powerups :speed)]
      (assoc this :pos (get-next-pos pos direction delta-time speed)))))

(defrecord World [game-map player])

(defn move-player!
  [world-atom direction delta-time]
  (let [new-player (move (:player @world-atom) direction delta-time)
        cells (get-cells-inside (:game-map @world-atom) (get-bounding-box new-player))]
    (when (every? #(not (:is-obstacle %)) cells)
      (swap! world-atom assoc-in [:player] new-player))))

(defn run-game-loop!
  [world-atom input-chan]
  (go
    (loop [direction (<! input-chan)]
      (move-player! world-atom direction 0.2)
      (recur (<! input-chan)))))

(defn- create-cell [type]
  (let [params (case type
                :empty {:obstacle false}
                :boundary {:obstacle true}
                :brick {:obstacle true}
                :obstacle {:obstacle true})]
  (->Cell type (:obstacle params))))

(defn- create-player [pos-x pos-y lives]
  (->Player [pos-x pos-y]
            lives
            {:speed 1.0}
            0.45
            0.45))

(defn- create-map [obstacle-columns obstacle-rows]
  (let [inner-width (+ 1 (* 2 obstacle-columns))
        inner-height (+ 1 (* 2 obstacle-rows))
        total-width (+ 2 inner-width)]
    (->Map
      (concat
        ; Top obstacle row
        [(map #(create-cell :boundary) (range total-width))]

        (for [row-i (range inner-height)
              :let [is-obstacle-row (odd? row-i)]]
           (concat
             [(create-cell :boundary)]
             (for [col-i (range inner-width)
                   :let [is-obstacle-col (odd? col-i)]]
               (if (and is-obstacle-col is-obstacle-row)
                 (create-cell :obstacle)
                 (create-cell :empty)))
             [(create-cell :boundary)]))

        ; Bottom obstacle row
        [(map #(create-cell :boundary) (range total-width))]))))

(defn create []
  (atom
    (->World (create-map 9 4)
             (create-player 1.5 1.5 3))))
