(ns demo.pizza
  (:require [kakkonen.core :as k]
            [kakkonen.dev :as dev]
            [malli.generator :as mg]
            [malli.util :as mu]))

(def Pizza
  [:map {:title "pizza"}
   [:id :int]
   [:name :string]
   [:size [:enum "S" "M" "L"]]
   [:price :int]
   [:origin [:map
             [:country :string]]]])

(def dispatcher
  (dev/dispatcher
   {:pizza/list {:kind :query
                 :description "list pizzas"
                 :permissions #{:pizza/read}
                 :output [:vector Pizza]
                 :handler (fn [_env _ctx _]
                            (mg/generate [:vector Pizza]))}

    :pizza/get {:kind :query
                :input [:map [:id int?]]
                :permissions #{:pizza/read}
                :output [:maybe Pizza]
                :handler (fn [_env _ctx {:keys [id]}]
                           (mg/generate [:maybe {:gen/fmap #(some-> % (assoc :id id))} Pizza]))}

    :pizza/add {:kind :command
                :description "add a pizza"
                :permissions #{:pizza/write}
                :input (mu/dissoc Pizza :id)
                :output Pizza
                :handler (fn [_env _ctx pizza] (assoc pizza :id 1))}

    :pizza/clear {:kind :command
                  :description "remove pizzas"
                  :permissions #{:pizza/write}
                  :handler (fn [_env _ctx _data])}

    :pizza/nop {:kind :query}

    :system/actions {:kind :query
                     :permissions #{:system/read}
                     :handler k/-actions-handler}

    :system/available-actions {:kind :query
                               :permissions #{:system/read}
                               :handler k/-available-actions-handler}

    :system/stop {:kind :command
                  :permissions #{:system/write}}}

   {:modules [;; schema & docs
              (k/-assoc-type-module)
              (k/-documentation-module)
              (k/-kind-module {:values #{:command :query}})
              ;; guards
              (k/-permissions-module
               {:permissions #{:pizza/read :pizza/write :system/read :system/write}
                :get-permissions #(-> % :user :permissions (or #{}))})
              (k/-validate-input-module)
              (k/-validate-output-module)
              ;; invoke
              (k/-invoke-handler-module)]}))

(defn ! [event]
  (k/dispatch
   dispatcher
   nil
   {:user {:permissions #{:pizza/read :pizza/write :system/read}}}
   event))

(k/dry-run
 dispatcher
 nil
 {:user {:permissions #{:pizza/read :system/read}}}
 [:pizza/list])

(k/dispatch
 dispatcher
 nil
 {:user {:permissions #{:pizza/read :system/read}}}
 [:system/actions])

(k/dispatch
 dispatcher
 nil
 {:user {:permissions #{:pizza/read :system/read}}}
 [:system/available-actions])

(comment
 (! [:pizza/list])
 (! [:pizza/listz])
 (! [:pizza/get {:id 100}])
 (! [:pizza/add {:name "quatro"
                 :size "L"
                 :price 120
                 :origin {:country "Italy"}}])
 (! [:pizza/add {:name "quatro"}])
 (! [:pizza/clear])
 (! [:pizza/nop])
 (! [:system/actions])
 (! [:system/stop])
 (! [:system/available-actions]))

(comment

 (spit
  ".cpcache/pizza.puml"
  ((requiring-resolve 'plantuml/transform)
   dispatcher
   {:user {:description "Normal User"
           :permissions #{:pizza/read :system/read}}
    :admin {:description "System Admin"
            :permissions #{:pizza/read :pizza/write :system/read}}})))
