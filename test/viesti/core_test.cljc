(ns viesti.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [malli.core :as m]
            [viesti.core :as v]))

(def data
  {::inc {:handler (fn [{:keys [value]} _ _] (swap! value (fnil inc 0)))}
   ::plus {:handler (fn [{:keys [value]} _ x] (swap! value (fnil + 0) x))}
   ::get {:handler (fn [{:keys [value]} _ _] @value)}
   ::nop {}})

(def d (v/dispatcher data))

(deftest -fail-test
  (try
    (v/-fail! ::v/kikka)
    (catch #?(:clj Exception, :cljs js/Error) e
      (is (= ::v/kikka (-> e ex-data :type))))))

(deftest dispatcher-test
  (testing "accumulated schema"
    (is (= (m/form [:map
                    {:closed true}
                    [:description {:optional true} :string]
                    [:input {:optional true} :any]
                    [:output {:optional true} :any]
                    [:handler {:optional true} v/Handler]])
           (m/form (v/-schema d)))))

  (testing "types"
    (is (= #{::inc ::plus ::get ::nop} (v/-types d))))

  (testing "actions"
    (let [actions (v/-actions d)]
      (is (= #{::inc ::plus ::get ::nop} (set (keys actions))))
      (doseq [[k _] data] (is (-> k actions :handler)))))

  (testing "dispatching"
    (let [env {:value (atom 0)}
          ! (fn [event] (v/dispatch d env nil event))]

      (testing "succesful"
        (is (! [::inc]))
        (is (! {:type ::inc}))

        (is (! [::plus 3]))
        (is (! {:type ::plus :data 3}))

        (is (= 8 (! [::get])))
        (is (= 8 (! {:type ::get}))))

      (testing "invalid key throws"
        (is (thrown? #?(:clj Exception, :cljs js/Error) (! [::invalid])))
        (is (thrown? #?(:clj Exception, :cljs js/Error) (! {:type ::invalid}))))

      (testing "no handler does not throw"
        (is (nil? (! [::nop])))
        (is (nil? (! {:type ::nop})))))))

(deftest dispatcher-creation-test

  (testing "no actions are required"
    (is (v/dispatcher {})))

  (testing "handler is not required"
    (is (v/dispatcher {::inc {}})))

  (testing "extra keys are not accepted"
    (is (thrown?
         #?(:clj Exception, :cljs js/Error)
         (v/dispatcher {::inc {:title "123"}})))))

(deftest custom-dispatcher-test
  (let [d (v/dispatcher
           {::read {:kind :query
                    :features #{:feature/a}
                    :permissions #{:test/read}
                    :handler (fn [_ _ _] "read")}
            ::write {:kind :query
                     :permissions #{:test/write}
                     :handler (fn [_ _ _] "write")}
            ::list {:kind :query
                    :handler v/-actions-handler}
            ::available {:kind :query
                         :handler v/-available-actions-handler}}
           {:modules [(v/-assoc-type-module)
                      (v/-documentation-module)
                      (v/-kind-module {:values #{:command :query}})
                      (v/-features-module {:features #{:feature/a}
                                           :required false
                                           :get-features (fn [env _ _] (-> env :features (or #{})))})
                      (v/-permissions-module {:permissions #{:test/read :test/write}
                                              :required false
                                              :get-permissions (fn [_ ctx _] (-> ctx :user :permissions (or #{})))})
                      (v/-validate-input-module)
                      (v/-validate-output-module)
                      (v/-handler-module)]})]

    (testing "accumulated schema"
      (is (= (m/form [:map
                      {:closed true}
                      [:description {:optional true} :string]
                      [:kind [:enum :command :query]]
                      [:features {:optional true} [:set [:enum :feature/a]]]
                      [:permissions {:optional true} [:set [:enum :test/read :test/write]]]
                      [:input {:optional true} :any]
                      [:output {:optional true} :any]
                      [:handler {:optional true} v/Handler]])
             (m/form (v/-schema d)))))

    (testing "user with read permissions"
      (let [env {:features #{:feature/a}}
            ctx {:user {:permissions #{:test/read}}}]

        (testing "can dispatch to read"
          (is (nil? (v/check d env ctx [::read])))
          (is (nil? (v/dry-run d env ctx [::read])))
          (is (= "read" (v/dispatch d env ctx [::read]))))

        (testing "can't dispatch to write"
          (is (= {:data {:expected #{:test/write}
                         :type ::write
                         :missing #{:test/write}
                         :permissions #{:test/read}}
                  :type ::v/missing-permissions}
                 (v/check d env ctx [::write])))
          (is (= {:data {:expected #{:test/write}
                         :type ::write
                         :missing #{:test/write}
                         :permissions #{:test/read}}
                  :type ::v/missing-permissions}
                 (v/dry-run d env ctx [::write])))
          (is (thrown?
               #?(:clj Exception, :cljs js/Error)
               (v/dispatch d env ctx [::write]))))

        (testing "list actions"
          (is (= {::available {:kind :query, ::v/type ::available}
                  ::list {:kind :query, ::v/type ::list}
                  ::read {:kind :query, ::v/type ::read, :features #{:feature/a} :permissions #{:test/read}}
                  ::write {:kind :query, ::v/type ::write, :permissions #{:test/write}}}
                 (v/dispatch d env ctx [::list]))))

        (testing "list available commands"
          (testing "all good"
            (is (= {::available nil
                    ::list nil
                    ::read nil
                    ::write {:data {:expected #{:test/write}
                                    :type ::write
                                    :missing #{:test/write}
                                    :permissions #{:test/read}}
                             :type ::v/missing-permissions}}
                   (v/dispatch d env ctx [::available]))))
          (testing "missing feature flag"
            (is (= {::available nil
                    ::list nil
                    ::read {:data {:expected #{:feature/a}
                                   :features #{}
                                   :missing #{:feature/a}
                                   :type :viesti.core-test/read}
                            :type ::v/missing-features}
                    ::write {:data {:expected #{:test/write}
                                    :type ::write
                                    :missing #{:test/write}
                                    :permissions #{:test/read}}
                             :type ::v/missing-permissions}}
                   (v/dispatch d nil ctx [::available])))))))))
