(ns diffractor.util
  (:require [clojure.pprint :refer [pprint]]))

(defn count-attack [board]
  (let [attacking-units (:units (get board (:current_player board)))]
    (reduce #(+ %1 (:attack %2)) 0 attacking-units)))

(defn get-defender-units [board]
  (let [active-player (:current_player board)
        passive-player (if (= 1 active-player) 2 1)])
  (:units (get board passive-player)))

(defn count-defense [board]
  (let [defender-units (get-defender-units board)]
    (reduce #(+ %1 (if (= true (:can_block %2)) (:hp %2) 0))
            0
            defending-units)))

; "mutating" functions
(defn kill-breached-defense [board]
  nil)
