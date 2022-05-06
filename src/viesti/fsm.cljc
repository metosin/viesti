(ns viesti.fsm
  (:refer-clojure :exclude [-next])
  (:require [clojure.walk :as walk]
            [malli.core :as m]
            [viesti.core :as v]))

;;
;; helpers
;;

(defn -reduce-map [m ns keep]
  (reduce-kv
   (fn [m k v] (if (= ns (namespace k)) (if keep (assoc m (keyword (name k)) v) m) (if keep m (assoc m k v)))) {} m))

(defn -reduce-over
  ([f x] (-reduce-over f x 1))
  ([f x i] (if (map? x) (reduce-kv (fn [m k v] (assoc m k (if (pos? i) (-reduce-over f v (dec i)) (f v)))) {} x) x)))

(defn -walk-map [f m] (walk/prewalk (fn [x] (cond-> x (map? x) f)) m))

(defn -normalize-signals [flow]
  (let [f (fn f [x ons]
            (if (map? x)
              (let [[x ons] (if (:fsm/initial x) [(dissoc x :fsm/on) (merge ons (:fsm/on x))] [x ons])]
                (reduce-kv
                 (fn [acc k v]
                   (assoc acc k
                          (if (= :fsm/states k)
                            (-reduce-over
                             (fn [m]
                               (cond-> m
                                 (not (:fsm/final m))
                                 (update :fsm/on (partial merge ons)))) v 0)
                            (f v ons)))) {} x)) x))]
    (f flow nil)))

(defn -normalize-permissions-and-features [flow]
  (let [mappings {:permissions (fnil into #{}), :features (fnil into #{})}
        push (fn [m]
               (reduce
                (fn [m [pk pm]]
                  (if-let [pv (m pk)]
                    (let [found (atom nil)]
                      (cond-> (reduce-kv
                               (fn [m k v]
                                 (if (map? v)
                                   (do (reset! found true)
                                       (assoc m k (update v pk pm pv)))
                                   m)) m (dissoc m pk)) @found (dissoc pk))) m)) m mappings))]
    (walk/prewalk (fn [x] (cond-> x (map? x) push)) flow)))

(defn -strip-handlers-and-guards [flow]
  (walk/prewalk (fn [x] (cond-> x (map? x) (dissoc :handler :guard))) flow))

(defn -schema-forms [flow]
  (walk/prewalk (fn [x] (cond-> x (:input x) (update :input m/form) (:output x) (update :input m/form))) flow))

(defn -statechart [flow]
  (let [f (fn [m]
            (reduce-kv
             (fn [m k v]
               (let [nk (name k)]
                 (if (and (qualified-keyword? k) (= "fsm" (namespace k)))
                   (let [v (if (#{"on" "states"} nk)
                             (reduce-kv (fn [m k v] (assoc m (keyword "fsm" (name k)) v)) {} v) v)]
                     (assoc m (keyword (name k)) v))
                   m))) {} m))]
    (walk/prewalk (fn [x] (cond-> x (map? x) f)) flow)))

(defn -normalize [flow]
  (-> flow (-normalize-permissions-and-features) (-normalize-signals)))

(defn -with-dispatcher [flow {:keys [dispatcher] :or {dispatcher v/dispatcher} :as options}]
  (let [f (fn [m]
            (if-let [on (:fsm/on m)]
              (let [actions (reduce-kv (fn [acc k v] (assoc acc k (assoc (-reduce-map v "fsm" false) :kind :command))) {} on)]
                (assoc m :fsm/dispatcher (dispatcher actions options)))
              m))]
    (->> flow (-normalize) (-walk-map f))))

;;
;; Flow
;;

(defprotocol FlowRegistry
  (-get-flow [this id])
  (-get-flows [this]))

(defprotocol Flow
  (-id [this])
  (-initial [this])
  (-states [this])
  (-next [this state])
  (-flow [this]))

(defprotocol FlowMachine
  (-initialize [this env ctx])
  (-state [this id])
  (-available [this env ctx])
  (-check [this env ctx state])
  (-transition [this env ctx state event])
  (-machine [this]))

(defn -registry [fsms]
  (let [data (->> fsms (map (fn [fsm] [(-id fsm) fsm])) (into {}))]
    (reify FlowRegistry
      (-get-flow [_ id] (or (data id) (v/-fail! ::fsm-not-found {:id id})))
      (-get-flows [_] data))))

(defn -create [flow]
  (let [normalized (-normalize flow)]
    (reify
      Flow
      (-id [_] (:fsm/id normalized))
      (-initial [_] (:fsm/initial normalized))
      (-states [_] (:fsm/states normalized))
      (-flow [_] flow)
      (-next [this state]
        (-> (-states this) (get state) :fsm/on)))))

