(ns bomberman.world)


;; Constants

(def directions
  "Available directions"
  {:left [-1 0]
   :right [1 0]
   :top [0 -1]
   :bottom [0 1]})

(def obstacle-types
  #{:obstacle :brick})

(defn- rand-direction
  ([] (rand-direction directions))
  ([directions-map] (rand-nth (keys directions-map))))

(defn- horizontal? [direction] (#{:left :right} direction))

(def bomb-detonate-seconds 3)
(def flame-lifetime-seconds 1)
(def creep-speed 0.05)

;; Data types

(defrecord Player [pos direction speed powerups])
(defrecord Creep [pos direction])
(defrecord World [player cells bombs creeps flames])

(defn- create-obstacle-cell
  ([] (create-obstacle-cell :obstacle))
  ([type] {:type type}))

(defn- create-bomb-cell []
  {:type :bomb
   :timer-value bomb-detonate-seconds
   :collides? false})

(defn- create-flame-cell []
  {:type :flame
   :lifetime flame-lifetime-seconds})

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
                  (if (> (rand) 0.7)
                    (create-obstacle-cell :brick)
                    nil)))
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

(defn- get-next-pos [pos direction]
  (let [center-pos (map (partial + 0.5) pos)
        corner-dxdy (map (partial * 0.5) (directions direction))
        corner-pos (map + center-pos corner-dxdy)]
    (map Math/floor corner-pos)))

(defn get-next-cell [world pos direction]
  (get-cell world (get-next-pos pos direction)))

(defn- try-move
  [world pos speed direction]
  (let [dxdy (map (partial * speed) (directions direction))
        new-pos (map + pos dxdy)
        [new-x new-y] new-pos
        target-cell (get-next-cell world new-pos direction)]
    (if (or (nil? target-cell)
            (false? (:collides? target-cell)))
      (if (horizontal? direction)
        [nil [new-x (Math/round new-y)]]
        [nil [(Math/round new-x) new-y]])
      [:obstacle nil])))

(defn touches-player?
  [world cell-pos]
  (let [[cell-x cell-y] cell-pos
        [player-x player-y] (get-in world [:player :pos])]
    (and (< (- cell-x 1) player-x (+ cell-x 1))
         (< (- cell-y 1) player-y (+ cell-y 1)))))

;; Updaters

(declare detonate-bomb)
(declare remove-flame)

(defn- update-bombs-collides
  [world]
  (reduce
    (fn [world bomb-pos]
      (let [[bomb-x bomb-y] bomb-pos
            bomb-collidable? (:collides? (get-cell world bomb-pos))]
        (if (and (not bomb-collidable?)
                 (not (touches-player? world bomb-pos)))
          (assoc-in world [:cells bomb-y bomb-x :collides?] true)
          world)))
    world
    (:bombs world)))

(defn- calc-flame-cells [world cell-pos radius]
  (concat
    [cell-pos]
    (let []
      (for [direction (keys directions)
            i (range 1 radius)
            :let [cell-traverser #(map + % (directions direction))
                  cell-pos (nth (iterate cell-traverser cell-pos) i)]
            :while (and (every? #(>= % 0) cell-pos)
                        (not= :obstacle (:type (get-cell world cell-pos))))]
        cell-pos))))

(defn- update-bombs-timers
  [world delta-time]
  (reduce
    (fn [world bomb-pos]
      (let [[bomb-x bomb-y] bomb-pos
            current-value (:timer-value (get-cell world bomb-pos))
            new-value (- current-value (/ delta-time 1000))]
        (if (neg? new-value)
          (detonate-bomb world bomb-pos)
          (assoc-in world [:cells bomb-y bomb-x :timer-value] new-value))))
    world
    (:bombs world)))

(defn- update-creeps
  [world delta-time]
  (reduce
    (fn [world creep-i]
      (let [{pos :pos direction :direction} (get-in world [:creeps creep-i])
            [_ new-pos] (try-move world pos creep-speed direction)
            touches-player (touches-player? world new-pos)]
        (if (or (nil? new-pos) touches-player)
          (let [new-direction (rand-direction (dissoc directions direction))]
            (when touches-player (.log js/console "ouch!"))
            (assoc-in world [:creeps creep-i :direction] new-direction))
          (assoc-in world [:creeps creep-i :pos] new-pos))))
    world
    (range (count (:creeps world)))))

(defn- update-flames
  [world delta-time]
  (reduce
    (fn [world flame-pos]
      (let [[flame-x flame-y] flame-pos
            current-lifetime (:lifetime (get-cell world flame-pos))
            new-lifetime (- current-lifetime (/ delta-time 1000))]
        (if (neg? new-lifetime)
          (remove-flame world flame-pos)
          (assoc-in world [:cells flame-y flame-x :lifetime] new-lifetime))))
    world
    (:flames world)))

;; Public API

(defn step
  ""
  [world delta-time]
  (-> world
      (update-bombs-collides)
      (update-bombs-timers delta-time)
      (update-flames delta-time)
      (update-creeps delta-time)))

(defn move-player
  "Moves player inside world in given direction, returns new world"
  [{{:keys [speed pos]} :player :as world} direction]
  (let [[obstacle-type new-pos] (try-move world pos speed direction)]
    (case obstacle-type
      nil (assoc-in world [:player :pos] new-pos)
      :obstacle world
      :flame )))

(defn place-bomb
  "Spawns new bomb at player's position, returns new world"
  [{player :player :as world}]
  (let [[col-i row-i] (get-pos-cell (:pos player))]
    (-> world
      (update-in [:bombs] conj [col-i row-i])
      (assoc-in [:cells row-i col-i] (create-bomb-cell)))))

(defn spawn-creep
  "Adds new creep at given position, returns new world"
  [world pos]
  (let [creep-cell (get-pos-cell pos)
        creep (->Creep creep-cell (rand-direction))]
    (update-in world [:creeps] conj creep)))

(defn add-flames [world center-pos radius]
  "Adds bomb explosion flames at given position, returns new world"
  (reduce
    (fn [world [x y]]
      (-> world
          (assoc-in [:cells y x] (create-flame-cell))
          (update-in [:flames] conj [x y])))
    world
    (calc-flame-cells world center-pos radius)))

(defn detonate-bomb
  "Detonates existing bomb at given position, returns new world"
  [world [bomb-x bomb-y]]
  (-> world
      (add-flames [bomb-x bomb-y] 3)
      (update-in [:bombs] disj [bomb-x bomb-y])))

(defn remove-flame
  [world [flame-x flame-y]]
  (-> world
      (assoc-in [:cells flame-y flame-x] nil)
      (update-in [:flames] disj [flame-x flame-y])))

(defn create
  "Creates world instance"
  []
  (let [player (create-player [1 1] :bottom 0.06 {:lives 4})
        world (->World player (generate-cells 5 5) #{} [] #{})]
    (-> world
        (spawn-creep [3 3])
        (spawn-creep [3 5])
        (spawn-creep [5 3])
        (spawn-creep [5 5]))))