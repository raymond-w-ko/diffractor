(ns diffractor.util
  (:require [clojure.pprint :refer [pprint]]))

(defn count-attack [board]
  (let [attacking-units (:units (get board (:current_player board)))]
    (reduce #(+ %1 (:attack %2)) 0 attacking-units)))

(defn get-attacking-player-number [board]
  (:current_player board))

(defn get-defending-player-number [board]
  (if (= 1 (:current_player board))
    2
    1))

(defn get-attacking-player [board]
  (get board (:current_player board)))

(defn get-defending-player [board]
  (get board (get-defending-player-number board)))

(defn get-defender-units [board]
  (:units (get-defending-player board)))

(defn count-defense [board]
  (let [defender-units (get-defender-units board)]
    (reduce #(+ %1 (if (= true (:is_blocking %2)) (:hp %2) 0))
            0
            defender-units)))

;;; given a set of units and breach damage number, return a seq (list) of
;;; possible units that can remain
;;; for example, given [Drone, Tarsier], 1 as input
;;; returns [Drone], [Tarsier] as possible results
(defn generate-breaching-possibilities [units breach-damage]
  )

; "mutating" functions
(defn kill-breached-defense [board]
  (let [player-number (get-defending-player-number board)
        units (get-defender-units board)
        remaining-units (remove #((true? :is_blocking %)) units)]
    (assoc-in board [player-number :units] remaining-units)))

(defn get-breachable-units [units breach-damage]
  (filter #(<= (:hp %) breach-damage) units))
