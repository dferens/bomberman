(ns bomberman.world
  (:require [reagent.core :as reagent :refer [atom]]))

(def cell-types
  [:. ; empty space
   :obstacle ; can not be destroyed
   :boundary ; map borders
   :brick ; destroyable blocks
   ])

(defprotocol WorldProtocol
  (move-player! [this direction]))

(defprotocol PlayerProtocol
  (move [this direction]))

(defrecord Cell [type])
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
    (swap! player move delta-vec)))

(defn- create-cell [type]
  (atom (->Cell type)))

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
                 (create-cell :.)))
             [(create-cell :boundary)]))

        ; Bottom obstacle row
        [(map #(create-cell :boundary) (range total-width))]))))

(defn create []
  (->World (create-map 9 4)
           (create-player 1 1 3)))
