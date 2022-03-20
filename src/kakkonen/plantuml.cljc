(ns kakkonen.plantuml
  (:require [kakkonen.core :as k]
            [clojure.string :as str]))

(defn transform [dispatcher roles]
  (let [p->rs (->> (for [[r {:keys [permissions]}] roles, p permissions] [p r])
                   (reduce (fn [acc [r ps]] (update acc r (fnil conj #{}) ps)) {}))
        actions (sort-by ::k/type (vals (k/-actions dispatcher)))
        << #(str "(" % ")"), <<t #(str/replace % #"\s+" ""), <<r #(-> % roles :description), >> println]
    (with-out-str
      (>> "@startuml")
      (>> "left to right direction")
      (doseq [[k _] roles] (>> "actor" (str "\"" (<<r k) "\"") "as" (<<t (<<r k))))
      (doseq [[ns actions] (group-by #(-> % ::k/type namespace) actions)]
        (>> "rectangle" ns "{") (doseq [{:keys [::k/type]} actions] (println " " (<< type))) (println "}"))
      (doseq [{:keys [::k/type permissions]} actions, p permissions, r (sort (p->rs p))]
        (>> (<<t (<<r r)) "-->" (<< type)))
      (>> "@enduml"))))
