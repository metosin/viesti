(ns viesti.generator
  (:require [malli.generator :as mg]
            [viesti.core :as v]))

(defn -accept-no-handler [_ {:keys [handler]} _] (boolean (not handler)))
(defn -accept-output [_ {:keys [output]} _] (boolean output))
(defn -accept-all [_ _ _] true)

(defn -gen-output [_ {:keys [output]} _]
  (let [gen (mg/generator (or output :nil))]
    (fn [& _args] (mg/generate gen))))

(defn -generate-output-module
  ([] (-generate-output-module nil))
  ([{:keys [filters gen] :or {filters [-accept-no-handler], gen -gen-output}}]
   {:name '-generate-output-module
    :compile (fn [type data options]
               (when (or (not filters) (some #(% type data options) filters))
                 (when-let [handler (and gen (gen type data options))]
                   (-> data
                       (assoc ::generated true)
                       (update ::v/middleware (fnil conj []) (constantly handler))))))}))
