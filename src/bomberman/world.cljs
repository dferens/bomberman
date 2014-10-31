(ns bomberman.world
  (:require [cljs.core.async :refer [chan <! put! timeout alts!]]
            [jayq.util :refer [log]]
            [reagent.core :as reagent :refer [atom]])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(def DIRECTIONS
  {:left [-1 0] :right [1 0]
   :top [0 -1] :bottom [0 1]
   :none [0 0]})

(defn get-next-pos
  ([pos direction delta-time]
   (next-pos pos direction delta-time 1))
  ([pos direction delta-time speed]
    (let [delta-time-seconds (/ delta-time 1000)]
     (map #(+ %1 (* %2 delta-time-seconds speed)) pos (get DIRECTIONS direction)))))

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

(defrecord Player [pos lives powerups width height direction]
  PlayerProtocol
  (get-bounding-box
    [this]
    {:x (first pos) :y (second pos)
     :width width :height height})
  (move
    [this direction delta-time]
    (let [speed (-> this :powerups :speed)]
      (-> this
          (assoc :pos (get-next-pos pos direction delta-time speed))
          (assoc :direction direction)))))

(defrecord Bomb [x y])

(defrecord World [game-map player bombs ups time updates])

(defn move-player!
  [world direction delta-time]
  (let [new-player (move (:player world) direction delta-time)
        cells (get-cells-inside (:game-map world) (get-bounding-box new-player))]
    (if (every? #(not (:is-obstacle %)) cells)
      (assoc-in world [:player] new-player)
      world)))

(defn- place-bomb!
  [world]
  (log "placed bomb")
  world)

(defn- world-updater
  [world delta-time msg]
  (let [new-direction (if (= (:topic msg) :move)
                              (:direction msg)
                              (get-in world [:player :direction]))]
   (as-> world $
          ; Update UPS value
          (if (> (:time $) 1000)
            (-> $
                (assoc :ups (/ (:updates $) (/ 1000 (:time $))))
                (merge {:time 0 :updates 0}))
            (-> $
                (update-in [:time] + delta-time)
                (update-in [:updates] inc)))

          ; Update player
          (move-player! $ new-direction delta-time)

          (case (:topic msg)
            :place-bomb (place-bomb! $)
            $))))

(defn run-game-loop!
  [world-atom input-chan]
  (let [delta-time 20]
    (go
      (loop []
        (<! (timeout delta-time))
        (let [[msg _] (alts! [input-chan (timeout 0)])]
          (swap! world-atom world-updater delta-time msg)
          (recur))))))

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
            0.4
            0.4
            :none))

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
             (create-player 1.5 1.5 3)
             []
             0
             0
             0)))
