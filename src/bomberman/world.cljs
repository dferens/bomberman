(ns bomberman.world
  (:require [cljs.core.async :refer []]
            [reagent.core :as reagent :refer [atom]])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(def DIRECTIONS
  {:left [-1 0] :right [1 0]
   :top [0 -1] :bottom [0 1]})

(defprotocol WorldProtocol
  (move-player! [this direction])
  (run-game-loop! [this input-chan]))

(defprotocol PlayerProtocol
  (move [this direction]))

(defrecord Cell [type is-obstacle])
(defrecord Map [cells])
(defrecord Player [pos lives powerups]
  PlayerProtocol
  (move
    [this delta-vec]
    (let [speed (-> this :powerups :speed)
          new-pos (map #(+ %1 (* speed %2)) pos delta-vec)]
      (assoc this :pos new-pos))))

(defrecord World [map player]
  WorldProtocol
  (move-player!
    [this delta-vec]
    (swap! player move delta-vec))
  (run-game-loop!
    [this input-chan]
    (go
      (loop [direction (<! input-chan)]
        (move-player! this (direction DIRECTIONS))
        (recur (<! input-chan))))))

(defn- create-cell [type]
  (let [params (case type
                :empty {:obstacle false}
                :boundary {:obstacle true}
                :brick {:obstacle true}
                :obstacle {:obstacle true})]
  (atom (->Cell type (:obstacle params)))))

(defn- create-player [pos-x pos-y lives]
  (atom (->Player [pos-x pos-y]
                  lives
                  {:speed 1.0})))

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
  (->World (create-map 9 4)
           (create-player 1 1 3)))
