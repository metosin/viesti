(ns demo.pizza
  (:require [malli.generator :as mg]
            [malli.util :as mu]
            [viesti.core :as v]
            [viesti.dev :as dev]))

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
                     :handler v/-actions-handler}

    :system/available-actions {:kind :query
                               :permissions #{:system/read}
                               :handler v/-available-actions-handler}

    :system/stop {:kind :command
                  :permissions #{:system/write}}}

   {:modules [;; schema & docs
              (v/-assoc-type-module)
              (v/-documentation-module)
              (v/-kind-module {:values #{:command :query}})
              ;; guards
              (v/-permissions-module
               {:permissions #{:pizza/read :pizza/write :system/read :system/write}
                :get-permissions (fn [_ ctx _] (-> ctx :user :permissions (or #{})))})
              (v/-validate-input-module)
              (v/-validate-output-module)
              ;; invoke
              (v/-invoke-handler-module)]}))

(defn ! [event]
  (v/dispatch
   dispatcher
   nil
   {:user {:permissions #{:pizza/read :pizza/write :system/read}}}
   event))

(v/dry-run
 dispatcher
 nil
 {:user {:permissions #{:pizza/read :system/read}}}
 [:pizza/list])

(v/dispatch
 dispatcher
 nil
 {:user {:permissions #{:pizza/read :system/read}}}
 [:system/actions])

(v/dispatch
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
  ((requiring-resolve 'viesti.plantuml/transform)
   dispatcher
   {:user {:description "Normal User"
           :permissions #{:pizza/read :system/read}}
    :admin {:description "System Admin"
            :permissions #{:pizza/read :pizza/write :system/read}}})))
