(ns viesti.plantuml-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [viesti.core :as v]
            [viesti.plantuml :as plantuml]))

(defn trim [s] (str/replace (str/trim s) #"\s+" " "))

(def roles
  {:user {:description "Normal User"
          :permissions #{:pizza/read :system/read}}
   :admin {:description "System Admin"
           :permissions #{:pizza/read :pizza/write :system/read}}})

(def dispatcher
  (v/dispatcher
   {:user/login {:kind :command}
    :pizza/list {:kind :query, :permissions #{:pizza/read}}
    :pizza/get {:kind :query, :permissions #{:pizza/read}}
    :pizza/add {:kind :command, :permissions #{:pizza/write}}
    :pizza/clear {:kind :command, :permissions #{:pizza/write}}
    :system/actions {:kind :query, :permissions #{:system/read}, :handler v/-actions-handler}
    :system/available-actions {:kind :query, :permissions #{:system/read}, :handler v/-available-actions-handler}}
   {:modules [(v/-assoc-type-module)
              (v/-kind-module {:values #{:command :query}})
              (v/-permissions-module
               {:permissions (->> roles (mapcat (comp :permissions val)) (set))})
              (v/-invoke-handler-module)]}))

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
