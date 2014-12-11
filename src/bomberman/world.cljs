(ns bomberman.world
  (:require [bomberman.collisions :as collisions :refer [collides?]]))


;; Constants

(def directions
  "Available directions"
  {:left [-1 0]
   :right [1 0]
   :top [0 -1]
   :bottom [0 1]
   :none [0 0]})

(def cell-types
  "Available cell types"
  #{:empty
    :obstacle
    :boundary})

(def cell-size [1 1])
(def player-size [0.7 0.7])
(def bomb-size [0.8 0.8])
(def bomb-detonate-seconds 3)


(defn- transpose
  "Moves point with given position in direction with given speed
  Returns new pos"
  [pos direction speed]
  (let [dxy (map (partial * speed) (directions direction))]
    (map + pos dxy)))

;; Data types

(defrecord Cell [pos type]
  collisions/ICollidable
  (get-bounding-box [this]
    (collisions/create-bounding-box pos cell-size)))

(defrecord Bomb [pos size collides timer-value]
  collisions/ICollidable
  (get-bounding-box [this]
    (collisions/create-bounding-box pos size)))

(defrecord Player [pos size direction speed powerups]
  collisions/ICollidable
  (get-bounding-box [this]
    (collisions/create-bounding-box pos size)))

(defrecord World [player cells bombs])

(defn- create-cell [cell-type col-i row-i]
  "Creates cell instance with given type & coordinates"
  {:pre (contains? cell-types cell-type)}
  (->Cell
    [(+ col-i (/ (first cell-size) 2))
     (+ row-i (/ (second cell-size) 2))]
    cell-type))

(defn- create-bomb [pos]
  (->Bomb
    pos
    bomb-size
    false
    bomb-detonate-seconds))

(defn- create-player
  "Creates player record where
  @pos - [x y]
  @size - [w h]"
  [pos direction speed powerups]
  (->Player pos
            player-size
            direction
            speed
            powerups))

(defn- gen-map-cells
  "Generates random map cells.
Example:
  (gen-map-cells 4 2) will generate next map

  [[x x x x x x x x x x x]
   [x . . . . . . . . . x]
   [x . x . x . x . x . x]
   [x . . . . . . . . . .]
   [x . x . x . x . x . x]
   [x . . . . . . . . . .]
   [x x x x x x x x x x x]]"
  [obstacle-rows obstacle-columns]
  (let [inner-width (+ 1 (* 2 obstacle-columns))
        inner-height (+ 1 (* 2 obstacle-rows))
        total-width (+ 2 inner-width)
        total-height (+ 2 inner-height)]
    (concat
      ; Top obstacle row
      [(map #(create-cell :boundary % 0) (range total-width))]

      (for [row-i (range inner-height)
            :let [cell-y (inc row-i)
                  is-obstacle-row (odd? row-i)]]
        (concat
          [(create-cell :boundary 0 cell-y)]
          (for [col-i (range inner-width)
                :let [cell-x (inc col-i)
                      is-obstacle-col (odd? col-i)]]
            (if (and is-obstacle-col is-obstacle-row)
              (create-cell :obstacle cell-x cell-y)
              (create-cell :empty cell-x cell-y)))
          [(create-cell :boundary total-width cell-y)]))

      ; Bottom obstacle row
      [(map #(create-cell :boundary % total-height) (range total-width))])))

(defn create
  "Creates world instance"
  []
  (->World
    (create-player [1.5 1.5]
                   :bottom
                   0.06
                   {:lives 4})
    (gen-map-cells 4 9)
    []))


;; Updaters

(defn- update-bombs-collides!
  [world]
  (doseq [bomb-atom (:bombs world)]
    (if (and (not (:collides @bomb-atom))
             (not (collides? (:player world) @bomb-atom)))
      (swap! bomb-atom assoc :collides true)))
  world)

(defn- update-bombs-timers!
  [{bombs :bombs :as world} delta-time]
  (doseq [bomb-atom bombs]
    (swap! bomb-atom update-in [:timer-value] - (/ delta-time 1000)))
  (assoc world :bombs (remove #(neg? (:timer-value (deref %))) bombs)))

(defn step
  [world delta-time]
  (-> world
      (update-bombs-collides!)
      (update-bombs-timers! delta-time)))

(defn move-player
  "Moves player inside world in given direction, returns new world"
  [{:keys [player] :as world} direction]
  {:pre (contains? directions direction)}
  (let [new-player (-> player
                       (update-in [:pos] transpose direction (:speed player))
                       (assoc :direction direction))
        bombs (filter :collides (map deref (:bombs world)))
        cells (->>(apply concat (:cells world))
                  (filter #(not= :empty (:type %))))
        bodies (->> (concat bombs cells)
                    (filter (partial collides? new-player)))]
    (if (empty? bodies)
      (assoc world :player new-player)
      world)))

(defn place-bomb
  [world]
  (let [bomb-pos (get-in world [:player :pos])
        bomb (create-bomb bomb-pos)]
    (update-in world [:bombs] conj (atom bomb))))