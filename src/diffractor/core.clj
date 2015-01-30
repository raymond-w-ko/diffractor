(ns diffractor.core
  (:require [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]
            [diffractor.util :refer :all]
            )
  (:use [clojure.walk])
  (:gen-class))

(def units-database)
(defn load-units-database []
  (let [text (slurp "units.json")
        data (json/read-str text)
        data (map #(clojure.walk/keywordize-keys %) data)
        data (reduce #(assoc %1 (:name %2) %2) {} data)]
    (def units-database data)
    ;(pprint data)
    ))

(def phases
  [
   ; defend against previous player
   :defend
   ; for resource generation
   :start
   ; for:
   ; clicking on drones (which may or may not happen),
   ; buying new units (which may or may not happen),
   ; clicking on unit abilities (which may or may not happen,
   ; basically huge amount of actions possible here !!!
   :build
   ; submit attack, which may result in a breach
   :commit-attack
   ; if we have attack >= defense, then we "breach", which wipes out oppenent defense
   :breach
   ; for clearing out transient resources
   :end-turn
   ])
(def next-phase (apply assoc {} (interleave phases
                                         (concat (rest phases) (list (first phases))))))

; TODO: load buildable units of the scenario
(defn load-scenario []
  (let [scenario-data (clojure.walk/keywordize-keys
                        (json/read-str
                          (slurp "defense_tactics_1.json")))
        player1 (:player1 scenario-data)
        player2 (:player2 scenario-data)
        resources1 (:resources player1)
        resources2 (:resources player2)
        units1 (map #(get units-database %) (:units player1))
        units2 (map #(get units-database %) (:units player2))]
    {:current_player (:current_player scenario-data)
     :parent nil
     1 {:resources resources1, :units units1}
     2 {:resources resources2, :units units2}}))

; TODO: implement resource generation
(defn expand-start-phase [parent-board]
  (let [board (into parent-board {:parent parent-board, :phase :build})]
    (list board)))

; TODO: implement the over 9000 click actions
(defn expand-build-phase [parent-board]
  (let [board (into parent-board {:parent parent-board, :phase :attack})]
  (list board)))

(defn expand-attack-phase [parent-board]
  (let [attack (count-attack parent-board)
        defense (count-defense parent-board)
        board (into parent-board
                    {:parent parent-board,
                     :phase :breach
                     :attack attack
                     :defense defense
                     :breach_left (- attack defense)})]
    (list board)))

(def expand-breach-phase [parent-board]
  (if (< 0 (:breach_left parent-board))
    ; no breach
    (list (into parent-board {:phase :defend}))
    ; breach
    (let [board (kill-breached-defense parent-board)
          defender-units (get-defender-units parent-board)
          breachable-units (filter #(%1 %2) defender-units)
          ])))

(def phase-expander
  {:start expand-start-phase
   :build expand-build-phase
   :attack expand-attack-phase
   :breach expand-breach-phase
   :defend nil
   })

(defn expander [boards]
  (let [phase (:phase (first boards))
        expander-fn (get phase-expander phase)]
    (if (nil? expander-fn)
      boards
      (recur (mapcat boards expander-fn)))))

(defn -main
  [& args]
  (load-units-database)
  (let [initial-board (list (load-scenario))]
    ;(pprint initial-board)
    ;(count-attack initial-board)
    ;(count-defense initial-board)
    ;(pprint (expand-start-phase initial-board))
    (expander initial-board)
    ))
