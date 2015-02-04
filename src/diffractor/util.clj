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

(defn print-board [board]
  (let [units2 (->> (:units (get board 2))
                    (map #(:name %))
                    (interpose " ")
                    (apply str))
        units1 (->> (:units (get board 1))
                    (map #(:name %))
                    (interpose " ")
                    (apply str))]
    (println "----------------------------------------")
    (println (:phase board) "Att" (:attack board) "Def" (:defense board) "Bre" (:breach-left board))
    (if (= (:current-player board) 1)
      (do
        (println "P2" units2)
        (println "P1" units1))
      (do
        (println "P1" units1)
        (println "P2" units2)))))

(defn print-boards [boards]
  (println "********************************************************************************")
  (doall (map print-board boards)))

(defn count-attack [board]
  (let [attacking-units (:units (get board (:current-player board)))]
    (reduce #(+ %1 (:attack %2)) 0 attacking-units)))

(defn get-attacking-player-index [board]
  (:current-player board))

(defn get-defending-player-index [board]
  (if (= 1 (:current-player board))
    2
    1))

(defn get-attacking-player [board]
  (get board (:current-player board)))

(defn switch-player-turns [board]
  (if (= 1 (get-attacking-player board))
    (assoc board :current-player 2)
    (assoc board :current-player 1)))

(defn get-defending-player [board]
  (get board (get-defending-player-index board)))

(defn get-defender-units [board]
  (:units (get-defending-player board)))

(defn count-defense [board]
  (let [defender-units (get-defender-units board)]
    (reduce #(+ %1 (if (= true (:blocking %2)) (:hp %2) 0))
            0
            defender-units)))

(defn kill-one-unit [units breach-damage]
  (fn [unit-to-kill]
    (list (remove-once (fn [arg] (= unit-to-kill arg)) units)
          (- breach-damage (:hp unit-to-kill)))))

;;; given a set of units and breach damage number, return a seq (list) of
;;; possible units that can remain
;;; for example, given [Drone, Tarsier], 1 as input
;;; returns ([Drone], [Tarsier]) as possible results
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

;;; similar to above function
(defn generate-defense-possibilities [units attack-damage]
  (if (= attack-damage 0)
    (list units)))

; "mutating" functions
(defn kill-breached-defense [board]
  (let [player-index (get-defending-player-index board)
        units (get-defender-units board)
        remaining-units (remove #((true? :blocking %)) units)]
    (assoc-in board [player-index :units] remaining-units)))

(defn get-breachable-units [units breach-damage]
  (filter #(<= (:hp %) breach-damage) units))
