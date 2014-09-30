(ns bomberman.world)

(def cell-types
  [:. ; empty space
   :obstacle ; can not be destroyed
   :boundary ; map borders
   :brick ; destroyable blocks
   ])

(defrecord Cell [type])
(defrecord Map [cells])
(defrecord World [map])

(defn- gen-map [obstacle-columns obstacle-rows]
  (let [inner-width (+ 1 (* 2 obstacle-columns))
        inner-height (+ 1 (* 2 obstacle-rows))
        total-width (+ 2 inner-width)]
    (->Map
      (concat
        ; Top obstacle row
        [(map #(->Cell :boundary) (range total-width))]

        (for [row-i (range inner-height)
              :let [is-obstacle-row (odd? row-i)]]
           (concat
             [(->Cell :boundary)]
             (for [col-i (range inner-width)
                   :let [is-obstacle-col (odd? col-i)]]
               (if (and is-obstacle-col is-obstacle-row)
                 (->Cell :obstacle)
                 (->Cell :.)))
             [(->Cell :boundary)]))

        ; Bottom obstacle row
        [(map #(->Cell :boundary) (range total-width))]))))

(defn gen-world []
  (->World (gen-map 9 4)))
