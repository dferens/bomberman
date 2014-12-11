(ns bomberman.collisions)


(defprotocol ICollidable
  (get-bounding-box [this]))


(defn create-bounding-box
  [center-x center-y width height]
  {:pos [(- center-x (/ width 2))
         (- center-y (/ height 2))]
   :size [width
          height]})

(defn create-bounding-box
  [center-point size]
  {:pos (map
          (fn [dim-i]
            (- (nth center-point dim-i)
               (/ (nth size dim-i) 2)))
          (range (count size)))
   :size size})


(defn collides? [collidable-a collidable-b]
  (let [box-a (get-bounding-box collidable-a)
        box-b (get-bounding-box collidable-b)]
    (if (or (nil? box-a) (nil? box-b))
      false
      (let [dims-count (count (:pos box-a))]
        (every?
          (fn [dim-i]
            (let [pos-a (nth (:pos box-a) dim-i)
                  pos-b (nth (:pos box-b) dim-i)
                  len-a (nth (:size box-a) dim-i)
                  len-b (nth (:size box-b) dim-i)]
              (and
                (> (+ pos-b len-b) pos-a)
                (< pos-b (+ pos-a len-a)))))
          (range dims-count))))))