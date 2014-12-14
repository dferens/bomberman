(ns bomberman.world)


;; Constants

(def bomb-detonate-seconds 3)

;; Data types

(defrecord Player [pos direction speed powerups])
(defrecord World [player cells bombs])

(defn- create-obstacle-cell []
  {:type :obstacle})

(defn- create-bomb-cell []
  {:type :bomb
   :timer-value bomb-detonate-seconds
   :collides? false})

(defn- generate-cells
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
        total-width (+ 2 inner-width)]
    (concat
      ; Top obstacle row
      [(repeat total-width (create-obstacle-cell))]

      (for [row-i (range inner-height)]
        (vec
          (concat
           [(create-obstacle-cell)]
           (for [col-i (range inner-width)]
             (if (and (odd? row-i) (odd? col-i))
               (create-obstacle-cell)
               nil))
           [(create-obstacle-cell)])))

      ; Bottom obstacle row
      [(repeat total-width (create-obstacle-cell))])))

(defn- create-player
  [pos direction speed powerups]
  (->Player pos
            direction
            speed
            powerups))

;; World orientation

(def directions
  "Available directions"
  {:left [-1 0]
   :right [1 0]
   :top [0 -1]
   :bottom [0 1]})

(defn- get-cell-coords [pos]
  (map Math/round pos))

(defn get-next-cell [world pos direction]
  (let [delta-cell (map #(* % 0.5) (directions direction))
        next-cell (map Math/round (map + pos delta-cell))
        [next-cell-x next-cell-y] next-cell]
    (-> world :cells (nth next-cell-y) (nth next-cell-x))))
;; Updaters

(defn- update-bombs-collides
  [{{[player-x player-y] :pos} :player :as world}]
  (reduce
    (fn [world [bomb-x bomb-y]]
      (let [bomb-collidable? (get-in (:cells world) [bomb-y bomb-x :collides?])
            touches-player? (and (< (- bomb-x 1) player-x (+ bomb-x 1))
                                 (< (- bomb-y 1) player-y (+ player-y 1)))]
        (if (and (not touches-player?) (not bomb-collidable?))
          (assoc-in world [:cells bomb-y bomb-x :collides?] true)
          world)))
    world
    (:bombs world)))

(defn- update-bombs-timers
  [world delta-time]
  (reduce
    (fn [world [bomb-x bomb-y]]
      (let [current-value (get-in (:cells world) [bomb-y bomb-x :timer-value])
            new-value (- current-value (/ delta-time 1000))]
        (if (neg? new-value)
          (-> world
              (assoc-in [:cells bomb-y bomb-x] nil)
              (update-in [:bombs] disj [bomb-x bomb-y]))
          (assoc-in world [:cells bomb-y bomb-x :timer-value] new-value))))
    world
    (:bombs world)))

;; Public API

(defn create
  "Creates world instance"
  []
  (->World
    (create-player [1 1] :bottom 0.06 {:lives 4})
    (vec (generate-cells 4 9))
    #{}))

(defn step
  ""
  [world delta-time]
  (-> world
      (update-bombs-collides)
      (update-bombs-timers delta-time)))


(defn move-player
  "Moves player inside world in given direction, returns new world"
  [{player :player :as world} direction]
  (let [dxdy (map (partial * (:speed player)) (directions direction))
        new-pos (map + (:pos player) dxdy)
        target-cell (get-next-cell world new-pos direction)]
    (if (or (nil? target-cell)
            (and (= (:type target-cell) :bomb)
                 (false? (:collides? target-cell))))
      (let [[new-x new-y] new-pos]
        (if (#{:left :right} direction)
          (assoc-in world [:player :pos] [new-x (Math/round new-y)])
          (assoc-in world [:player :pos] [(Math/round new-x) new-y])))
      world)))

(defn place-bomb
  "Spawns new bomb at player's position, returns new world"
  [{player :player :as world}]
  (let [[col-i row-i] (get-cell-coords (:pos player))]
    (-> world
      (update-in [:bombs] conj [col-i row-i])
      (assoc-in [:cells row-i col-i] (create-bomb-cell)))))