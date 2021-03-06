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

(defn construct-unit [data]
  {:post [%]}
  (if (= (type data) java.lang.String)
    (get units-database data)
    (let [unit-name (:name data)
          unit (get units-database unit-name)]
      (into unit data))))

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
        units1 (map #(construct-unit %) (:units player1))
        units2 (map #(construct-unit %) (:units player2))]
    ^{:parent nil}
    {:current-player (:current-player scenario-data)
     :phase (keyword (:phase scenario-data))
     1 {:resources resources1, :units units1}
     2 {:resources resources2, :units units2}}))

; TODO: implement resource generation
(defn expand-start-phase [parent-board]
  (let [board (into parent-board {:phase :build})
        board (with-meta board {:parent parent-board})]
    (list board)))

; TODO: implement the over 9000 click actions
(defn expand-build-phase [parent-board]
  (let [board (into parent-board {:phase :attack})
        board (with-meta board {:parent parent-board})]
  (list board)))

(defn expand-attack-phase [parent-board]
  (let [attack (count-attack parent-board)
        defense (count-defense parent-board)
        board (into parent-board
                    {:phase :breach
                     :attack attack
                     :defense defense
                     :breach-left (- attack defense)})
        board (with-meta board {:parent parent-board})]
    (list board)))

(defn simplify-units [units]
  (map #(:name %) units))

(defn expand-breach-phase [parent-board]
  (let [breach-damage (:breach-left parent-board)]
    (if (< breach-damage 0)
      ; no breach
      (list (into parent-board {:phase :defend
                                :breach-left 0}))
      ; breach
      (let [board (kill-breached-defense parent-board)
            defending-player-index (get-defending-player-index parent-board)
            defender-units (get-defender-units parent-board)
            breachable-units (get-breachable-units defender-units breach-damage)
            ,
            surviving-units-possibilities
            (generate-breaching-possibilities breachable-units breach-damage)
            ,
            boards (map #(assoc-in parent-board [defending-player-index :units] %)
                        surviving-units-possibilities)
            ]
        ;(pprint (map simplify-units surviving-units-possibilities))
        ;(pprint boards)
        (map #(into % {:phase :defend
                       :breach-left 0 ; breach damage is spent
                       :attack 0 ; attack must have been spent to breach
                       :defense 0 ; defense must have been wiped out to breach
                       })
             boards)))))

(defn expand-defend-phase [parent-board]
  (let [defender-units (get-defender-units parent-board)
        defending-player-index (get-defending-player-index parent-board)
        attack-damage (:attack parent-board)
        ,
        surviving-units-possibilities
        (generate-defense-possibilities defender-units attack-damage)
        ,
        boards (map #(assoc-in parent-board [defending-player-index :units] %)
                    surviving-units-possibilities)
        ]
    (map #(into % {:phase :start
                   :attack 0 ; attack must have been spent to breach
                   :defense 0 ; defense must have been wiped out to breach
                   :current-player (get-defending-player-index %)
                   })
         boards)))

(def phase-expander
  {:start expand-start-phase
   :build expand-build-phase
   :attack expand-attack-phase
   :breach expand-breach-phase
   :defend expand-defend-phase
   })

(defn expander [boards]
  (let [phase (:phase (first boards))
        expander-fn (get phase-expander phase)]
    (if (nil? expander-fn)
      boards
      (mapcat expander-fn boards))))

(defn -main
  [& args]
  (load-units-database)
  (loop [depth 0
         boards (list (load-scenario))]
    (print-boards boards)
    (when (< depth 10)
      (recur (inc depth) (expander boards))))
  )
