(ns bomberman.game
  (:require [bomberman.world]
            [cljs.core.async :refer [chan <! put!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(def commands
  #{:init
    :move
    :place-bomb
    :step})

(def responses
  #{:world-update})


(defrecord Game [commands-chan responses-chan])

(defn create []
  "Creates new game, returns [commands-chan responses-chan]"
  (let [commands-chan (chan)
        responses-chan (chan)
        world-atom (atom nil)]
    (go
      (loop [command (<! commands-chan)]

        (case (:topic command)
          :init (reset! world-atom (bomberman.world/create))
          :move (swap! world-atom bomberman.world/move-player (:direction command))
          :place-bomb (swap! world-atom bomberman.world/place-bomb)
          :step (do
                  (swap! world-atom bomberman.world/step (:delta-time command))
                  (put! responses-chan {:topic :world-update
                                        :world-state @world-atom})))

        (recur (<! commands-chan))))
    (->Game commands-chan responses-chan)))


(defn init!
  "Initializes game"
  [game]
  (put! (:commands-chan game) {:topic :init}))


(defn step!
  "Performs world iteration"
  [game delta-time]
  (put! (:commands-chan game) {:topic :step :delta-time delta-time}))


(defn move!
  [game direction]
  (put! (:commands-chan game) {:topic :move :direction direction}))

(defn place-bomb!
  [game]
  (put! (:commands-chan game) {:topic :place-bomb}))