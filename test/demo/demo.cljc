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
   {:pizza/list {:type :query
                 :description "list pizzas"
                 :permissions #{:pizza/read}
                 :output [:vector Pizza]
                 :handler (fn [_env _ctx _]
                            (mg/generate [:vector Pizza]))}

    :pizza/get {:type :query
                :input [:map [:id int?]]
                :permissions #{:pizza/read}
                :output [:maybe Pizza]
                :handler (fn [_env _ctx {:keys [id]}]
                           (mg/generate [:maybe {:gen/fmap #(some-> % (assoc :id id))} Pizza]))}

    :pizza/add {:type :command
                :description "add a pizza"
                :permissions #{:pizza/write}
                :input (mu/dissoc Pizza :id)
                :output Pizza
                :handler (fn [_env _ctx pizza] (assoc pizza :id 1))}

    :pizza/clear {:type :command
                  :description "remove pizzas"
                  :permissions #{:pizza/write}
                  :handler (fn [_env _ctx _data])}

    :pizza/nop {:type :query}

    :system/actions {:type :query
                     :permissions #{:system/read}
                     :handler k/-actions-handler}

    :system/available-actions {:type :query
                               :permissions #{:system/read}
                               :handler k/-available-actions-handler}

    :system/stop {:type :command
                  :permissions #{:system/write}}}

   {:modules [;; schema & docs
              (k/-assoc-key-module)
              (k/-documentation-module)
              (k/-cqrs-module)
              ;; guards
              (k/-permissions-module {:permissions #{:pizza/read :pizza/write :system/read :system/write}
                                      :get-permissions #(-> % :user :permissions (or #{}))})
              (k/-validate-input-module)
              (k/-validate-output-module)
              ;; invoke
              (k/-invoke-handler-module)]}))

(defn ! [event] (k/dispatch dispatcher nil {:user {:permissions #{:pizza/read :pizza/write :system/read}}} event))

(k/check
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
