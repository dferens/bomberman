(ns bomberman.math)


(def directions
  "Available directions"
  {:left [-1 0]
   :right [1 0]
   :top [0 -1]
   :bottom [0 1]})

; Vectors operations

(defn transpose
  "Moves point with given position in direction with given speed
  Returns new pos"
  [pos direction speed]
  {:pre (contains? directions direction)}
  (let [dxy (map (partial * speed) (directions direction))]
    (map + pos dxy)))
