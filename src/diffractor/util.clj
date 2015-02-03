(ns diffractor.util
  (:require [clojure.pprint :refer [pprint]]))

;;; thanks amalloy
;;; http://stackoverflow.com/questions/7662447/what-is-idiomatic-clojure-to-remove-a-single-instance-from-many-in-a-list
(defn remove-once [pred coll]
  ((fn inner [coll]
     (lazy-seq
       (when-let [[x & xs] (seq coll)]
         (if (pred x)
           xs
           (cons x (inner xs))))))
   coll))

(defn count-attack [board]
  (let [attacking-units (:units (get board (:current-player board)))]
    (reduce #(+ %1 (:attack %2)) 0 attacking-units)))

(defn get-attacking-player-number [board]
  (:current-player board))

(defn get-defending-player-number [board]
  (if (= 1 (:current-player board))
    2
    1))

(defn get-attacking-player [board]
  (get board (:current-player board)))

(defn get-defending-player [board]
  (get board (get-defending-player-number board)))

(defn get-defender-units [board]
  (:units (get-defending-player board)))

(defn count-defense [board]
  (let [defender-units (get-defender-units board)]
    (reduce #(+ %1 (if (= true (:is_blocking %2)) (:hp %2) 0))
            0
            defender-units)))

(defn kill-one-unit [units breach-damage]
  (fn [unit-to-kill]
    (list (remove-once (fn [arg] (= unit-to-kill arg)) units)
          (- breach-damage (:hp unit-to-kill)))))

;;; given a set of units and breach damage number, return a seq (list) of
;;; possible units that can remain
;;; for example, given [Drone, Tarsier], 1 as input
;;; returns [Drone], [Tarsier] as possible results
(defn generate-breaching-possibilities [units breach-damage]
  (if (= breach-damage 0)
    (list units)
    ;; We have to use some trickery here to avoid combinatory explosions of
    ;; branches. Like in defense tactics 1, if they have 5 tarsiers, we don't
    ;; want to treat each separate tarsier as a possible board state and have
    ;; to recognize that they are the same. Otherwise this will kill computers.
    (let [units (filter #(<= (:hp %) breach-damage) units)
          killable-units (distinct units)
          remaining-units-and-damage-list (map (kill-one-unit units breach-damage) killable-units)]
      (distinct (mapcat #(apply generate-breaching-possibilities %) remaining-units-and-damage-list)))))

; "mutating" functions
(defn kill-breached-defense [board]
  (let [player-number (get-defending-player-number board)
        units (get-defender-units board)
        remaining-units (remove #((true? :is_blocking %)) units)]
    (assoc-in board [player-number :units] remaining-units)))

(defn get-breachable-units [units breach-damage]
  (filter #(<= (:hp %) breach-damage) units))
