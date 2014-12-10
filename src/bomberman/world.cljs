(ns bomberman.world)


;; Constants

(def directions
  "Available directions"
  {:left [-1 0]
   :right [1 0]
   :top [0 -1]
   :bottom [0 1]
   :none [0 0]})

(def cell-types
  #{:empty :obstacle})


;; Builders

(defrecord Cell [x y type])
(defrecord Player [pos size direction speed powerups])

(defn- create-cell [cell-type x y]
  "Creates cell instance with given type & coordinates"
  {:pre (contains? cell-types cell-type)}
  (->Cell x
          y
          cell-type))

(defn- create-player
  "Creates player record where
  @pos - [x y]
  @size - [w h]
  "
  [pos size direction speed powerups]
  (->Player pos
            size
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
  {:gamemap (gen-map-cells 4 9)
   :player (create-player [1.0 1.0]
                          [0.7 0.7]
                          :bottom
                          0.06
                          {:lives 4})})


;; Updaters

(defn step [world delta-time]
  ; TODO: update game entities
  world)


(defn move-player
  "Moves player inside world in given direction, returns new world"
  [{:keys [player gamemap] :as world} direction]
  {:pre (contains? directions direction)}
  (let [dxdy (map (partial * (:speed player)) (directions direction))
        new-player (update-in player [:pos] #(map + % dxdy))
        top-left (:pos new-player)
        bottom-right (map + top-left (:size player))
        min-col (int (first top-left))
        min-row (int (second top-left))
        max-col (Math/ceil (first bottom-right))
        max-row (Math/ceil (second bottom-right))
        collided-cells (for [row-i (range min-row max-row)
                             col-i (range min-col max-col)]
                         (-> gamemap (nth row-i) (nth col-i)))]
    (if (every? #(= :empty (:type %)) collided-cells)
      (assoc world :player new-player)
      world)))