(ns bomberman.world)


;; Constants

(def directions
  "Available directions"
  {:left [-1 0]
   :right [1 0]
   :top [0 -1]
   :bottom [0 1]})

(defn- rand-direction
  ([] (rand-direction directions))
  ([directions-map] (rand-nth (keys directions-map))))

(defn- is-horizontal? [direction] (#{:left :right} direction))

(def bomb-detonate-seconds 3)
(def creep-speed 0.05)

;; Data types

(defrecord Player [pos direction speed powerups])
(defrecord Creep [pos direction])
(defrecord World [player cells bombs creeps])

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
    (vec
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
        [(repeat total-width (create-obstacle-cell))]))))

(defn- create-player
  [pos direction speed powerups]
  (->Player pos
            direction
            speed
            powerups))

(defn- get-pos-cell [pos]
  (map Math/round pos))

(defn- get-cell
  [world [cell-x cell-y]]
  (-> world :cells (nth cell-y) (nth cell-x)))

(defn get-next-cell [world pos direction]
  (let [delta-cell (map #(* % 0.5) (directions direction))
        next-cell-coords (map Math/round (map + pos delta-cell))]
    (get-cell world next-cell-coords)))

(defn- try-move
  [world pos speed direction]
  (let [dxdy (map (partial * speed) (directions direction))
        new-pos (map + pos dxdy)
        [new-x new-y] new-pos
        target-cell (get-next-cell world new-pos direction)]
    (if (or (nil? target-cell)
            (false? (:collides? target-cell)))
      (if (is-horizontal? direction)
        [new-x (Math/round new-y)]
        [(Math/round new-x) new-y])
      nil)))

(defn touches-player?
  [world cell-pos]
  (let [[cell-x cell-y] cell-pos
        [player-x player-y] (get-in world [:player :pos])]
    (and (< (- cell-x 1) player-x (+ cell-x 1))
         (< (- cell-y 1) player-y (+ cell-y 1)))))

;; Updaters

(defn- update-bombs-collides
  [world]
  (reduce
    (fn [world [bomb-x bomb-y]]
      (let [bomb-collidable? (get-in (:cells world) [bomb-y bomb-x :collides?])]
        (if (and (not bomb-collidable?)
                 (not (touches-player? world [bomb-x bomb-y])))
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

(defn- update-creeps
  [world delta-time]
  (reduce
    (fn [world creep-i]
      (let [{pos :pos direction :direction} (get-in world [:creeps creep-i])
            new-pos (try-move world pos creep-speed direction)
            touches-player (touches-player? world new-pos)]
        (if (or (nil? new-pos) touches-player)
          (let [new-direction (rand-direction (dissoc directions direction))]
            (when touches-player (.log js/console "ouch!"))
            (assoc-in world [:creeps creep-i :direction] new-direction))
          (assoc-in world [:creeps creep-i :pos] new-pos))))
    world
    (range (count (:creeps world)))))


(defn- spawn-creep
  [world pos]
  (let [creep-cell (get-pos-cell pos)
        creep (->Creep creep-cell (rand-direction))]
    (update-in world [:creeps] conj creep)))


;; Public API

(defn create
  "Creates world instance"
  []
  (let [player (create-player [1 1] :bottom 0.06 {:lives 4})
        world (->World player (generate-cells 10 15) #{} [])]
    (-> world
        (spawn-creep [3 3])
        (spawn-creep [3 5])
        (spawn-creep [5 3])
        (spawn-creep [5 5]))))

(defn step
  ""
  [world delta-time]
  (-> world
      (update-bombs-collides)
      (update-bombs-timers delta-time)
      (update-creeps delta-time)))


(defn move-player
  "Moves player inside world in given direction, returns new world"
  [{player :player :as world} direction]
  (let [new-pos (try-move world (:pos player) (:speed player) direction)]
    (if (not (nil? new-pos))
      (assoc-in world [:player :pos] new-pos)
      world)))

(defn place-bomb
  "Spawns new bomb at player's position, returns new world"
  [{player :player :as world}]
  (let [[col-i row-i] (get-pos-cell (:pos player))]
    (-> world
      (update-in [:bombs] conj [col-i row-i])
      (assoc-in [:cells row-i col-i] (create-bomb-cell)))))