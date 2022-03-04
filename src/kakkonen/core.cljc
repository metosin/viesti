(ns kakkonen.core
  (:require [malli.core :as m]
            [malli.util :as mu]
            [clojure.set :as set]))

;;
;; Protocols
;;

(defprotocol Dispatcher
  (-types [this])
  (-actions [this])
  (-schema [this])
  (-options [this])
  (-check [this env ctx event])
  (-dispatch [this env ctx event]))

;;
;; Schemas
;;

(def Env (m/schema [:maybe :map]))
(def Key (m/schema :qualified-keyword))
(def Data (m/schema [:maybe :map]))
(def Event (m/schema [:tuple Key Data]))
(def Options (m/schema [:maybe :map]))
(def Context (m/schema [:maybe :map]))
(def Response (m/schema :any))
(def Handler (m/schema [:=> [:cat Env Context Event] Response]))
(def Action (m/schema [:map {:closed true} [:handler {:optional true} Handler]]))
(def Mode (m/schema [:enum :invoke :check :validate]))

;;
;; Impl
;;

(defn -fail!
  ([type] (-fail! type nil))
  ([type data] (throw (ex-info (str type " " (pr-str data)) {:type type, :data data}))))

(defn -nil-handler [_env _ctx _data])

(defn -compile [action data {:keys [modules] :as options}]
  (reduce (fn [data {:keys [compile]}] (or (if compile (compile action data options)) data))
          (-> data (update :handler (fnil identity -nil-handler)) (assoc :checker -nil-handler))
          (reverse modules)))

(defn -ctx [ctx dispatcher validate invoke]
  (-> ctx (assoc ::dispatcher dispatcher) (assoc ::validate validate) (assoc ::invoke invoke)))

(defn -proxy [d f]
  (reify Dispatcher
    (-types [_] (-types d))
    (-actions [_] (-actions d))
    (-schema [_] (-schema d))
    (-options [_] (-options d))
    (-check [_ env ctx event] (-check d env ctx event))
    (-dispatch [_ env ctx event] (f env ctx event))))

;;
;; Modules
;;

(defn -invoke-handler-module []
  {:name '-invoke-handler-module
   :compile (fn [_ {:keys [handler] :as data} _]
              (assoc data :handler (fn [env ctx data]
                                     (let [f (if (::invoke ctx) handler -nil-handler)]
                                       (f env ctx data)))))})

(defn -assoc-key-module []
  {:name '-assoc-key-module
   :compile (fn [action data _] (assoc data ::key action))})

(defn -cqrs-module []
  {:name '-cqrs-module
   :schema [:map [:type [:enum :command :query]]]})

(defn -documentation-module []
  {:name '-documentation-module
   :schema [:map [:description {:optional true} :string]]})

(defn -validate-input-module []
  {:name '-validate-input-module
   :schema [:map [:input {:optional true} :any]]
   :compile (fn [key {:keys [input handler] :as data} _]
              (when input
                (let [validate (m/validator input)
                      explain (m/explainer input)
                      handler (fn [env ctx data]
                                (when (and (::validate ctx) (not (validate data)))
                                  (-fail! ::input-schema-error {:key key, :explanation (explain data)}))
                                (handler env ctx data))]
                  (assoc data :handler handler))))})

(defn -validate-output-module []
  {:name '-validate-output-module
   :schema [:map [:output {:optional true} :any]]
   :compile (fn [key {:keys [output handler] :as data} _]
              (when output
                (let [validate (m/validator output)
                      explain (m/explainer output)
                      handler (fn [env ctx data]
                                (if (::invoke ctx)
                                  (let [response (handler env ctx data)]
                                    (when-not (validate response)
                                      (-fail! ::output-schema-error {:key key, :explanation (explain response)}))
                                    response)
                                  (handler env ctx data)))]
                  (assoc data :handler handler))))})

(defn -permissions-module [{:keys [required permissions get-permissions] :or {get-permissions :permissions}}]
  {:name '-permissions-module
   :schema [:map [:permissions (if-not required {:optional true}) [:set (into [:enum] permissions)]]]
   :compile (fn [key {:keys [handler permissions] :as data} _]
              (when permissions
                (let [handler (fn [env ctx data]
                                (let [user-permissions (get-permissions ctx)]
                                  (let [missing (set/difference permissions user-permissions)]
                                    (if (seq missing)
                                      (-fail! ::missing-permissions {:permissions user-permissions
                                                                     :expected permissions
                                                                     :missing missing
                                                                     :key key})
                                      (handler env ctx data)))))]
                  (assoc data :handler handler))))})

;;
;; Default Handlers
;;

(defn -actions-handler [_ ctx _]
  (reduce-kv (fn [acc k v] (assoc acc k (dissoc v :handler))) {} (-actions (::dispatcher ctx))))

(defn -available-actions-handler [env {:keys [::dispatcher] :as ctx} _]
  (reduce-kv
   (fn [acc k data]
     (assoc acc k (-check dispatcher env (-ctx ctx dispatcher false false) [k data]))) {} (-actions dispatcher)))

;;
;; Defaults
;;

(defn -default-options []
  {:modules [(-assoc-key-module)
             (-documentation-module)
             (-validate-input-module)
             (-validate-output-module)
             (-invoke-handler-module)]})

;;
;; Public API
;;

(defn dispatcher
  ([actions] (dispatcher actions (-default-options)))
  ([actions {:keys [modules] :as options}]
   (let [schema (->> modules (keep :schema) (reduce mu/merge Action))]
     (when-let [explanation (m/explain [:map-of Key schema] actions)]
       (-fail! ::invalid-actions {:explanation explanation}))
     (let [compiled (reduce-kv (fn [acc key data] (assoc acc key (-compile key data options))) {} actions)
           types (set (keys actions))]
       (reify Dispatcher
         (-types [_] types)
         (-actions [_] actions)
         (-schema [_] schema)
         (-options [_] options)
         (-check [_ env ctx [key data]]
           (if-let [action (compiled key)]
             (let [handler (:handler action)]
               (try (handler env ctx data) (catch #?(:clj Exception, :cljs js/Error) e (ex-data e))))
             (-fail! ::invalid-action {:type key, :types types})))
         (-dispatch [_ env ctx [key data]]
           (if-let [action (compiled key)]
             (let [handler (:handler action)]
               (handler env ctx data))
             (-fail! ::invalid-action {:type key, :types types}))))))))

(defn check [dispatcher env ctx event] (-check dispatcher env (-ctx ctx dispatcher false false) event))
(defn dry-run [dispatcher env ctx event] (-check dispatcher env (-ctx ctx dispatcher true false) event))
(defn dispatch [dispatcher env ctx event] (-dispatch dispatcher env (-ctx ctx dispatcher true true) event))
