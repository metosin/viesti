(ns kakkonen.plantuml-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [kakkonen.core :as k]
            [kakkonen.plantuml :as plantuml]))

(defn trim [s] (str/replace (str/trim s) #"\s+" " "))

(def roles
  {:user {:description "Normal User"
          :permissions #{:pizza/read :system/read}}
   :admin {:description "System Admin"
           :permissions #{:pizza/read :pizza/write :system/read}}})

(def dispatcher
  (k/dispatcher
   {:user/login {:type :command}
    :pizza/list {:type :query, :permissions #{:pizza/read}}
    :pizza/get {:type :query, :permissions #{:pizza/read}}
    :pizza/add {:type :command, :permissions #{:pizza/write}}
    :pizza/clear {:type :command, :permissions #{:pizza/write}}
    :system/actions {:type :query, :permissions #{:system/read}, :handler k/-actions-handler}
    :system/available-actions {:type :query, :permissions #{:system/read}, :handler k/-available-actions-handler}}
   {:modules [(k/-assoc-key-module)
              (k/-cqrs-module)
              (k/-permissions-module
               {:permissions (->> roles (mapcat (comp :permissions val)) (set))})
              (k/-invoke-handler-module)]}))

(set (mapcat (comp :permissions val) roles))

(sort roles)

(deftest transform-test
  (is (= (trim
          "@startuml
           left to right direction
           actor \"Normal User\" as NormalUser
           actor \"System Admin\" as SystemAdmin
           rectangle pizza {
             (:pizza/add)
             (:pizza/clear)
             (:pizza/get)
             (:pizza/list)
           }
           rectangle system {
             (:system/actions)
             (:system/available-actions)
           }
           rectangle user {
             (:user/login)
           }
           SystemAdmin --> (:pizza/add)
           SystemAdmin --> (:pizza/clear)
           SystemAdmin --> (:pizza/get)
           NormalUser --> (:pizza/get)
           SystemAdmin --> (:pizza/list)
           NormalUser --> (:pizza/list)
           SystemAdmin --> (:system/actions)
           NormalUser --> (:system/actions)
           SystemAdmin --> (:system/available-actions)
           NormalUser --> (:system/available-actions)
           @enduml")
         (trim (plantuml/transform dispatcher roles)))))
