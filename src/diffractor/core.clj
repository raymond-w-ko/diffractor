(ns diffractor.core
  (:require [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]])
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
   ; for resource generation
   :player1-start
   ; for:
   ; clicking on drones (which may or may not happen),
   ; buying new units (which may or may not happen),
   ; clicking on unit abilities (which may or may not happen,
   ; basically huge amount of actions possible here !!!
   :player1-build
   ; submit attack, which may result in a breach
   :player1-commit-attack
   ; if we have attack >= defense, then we "breach", which wipes out oppenent defense
   :player1-breach
   ; for clearing out transient resources
   :player1-end-turn

   ; defend against player 1 attack
   :player2-defend
   :player2-start
   :player2-build
   :player2-commit-attack
   ; if we have attack >= defense, then we "breach", which wipes out oppenent defense
   :player2-breach
   ; for clearing out transient resources
   :player2-end-turn
   :player1-defend
   ])

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
    [{:resources resources1, :units units1}
     {:resources resources2, :units units2}]))

(defn -main
  [& args]
  (load-units-database)
  ; TODO: load buildable units
  (let [initial-board (load-scenario)]
    (pprint initial-board)))
