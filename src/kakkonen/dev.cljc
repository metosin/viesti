(ns kakkonen.dev
  (:require [kakkonen.core :as k]
            [malli.core :as m]
            [malli.dev.pretty :as pretty]
            [malli.dev.virhe :as v]
            [malli.error :as me]))

(defmethod v/-format ::k/invalid-action [_ _ {:keys [type types]} p]
  {:body [:group
          (pretty/-block "Dispatch value:" (v/-visit type p) p) :break :break
          (pretty/-block "Should be one of:" (v/-visit types p) p)]})

(defmethod v/-format ::k/no-handler [_ _ {:keys [type action]} p]
  {:body [:group
          (pretty/-block "No handler for type:" (v/-visit type p) p) :break :break
          (pretty/-block "Action:" (v/-visit action p) p)]})

(defmethod v/-format ::k/missing-permissions [_ _ {:keys [key permissions expected missing]} p]
  {:body [:group
          (pretty/-block "With action:" (v/-visit key p) p) :break :break
          (pretty/-block "Missing permission:" (v/-visit missing p) p) :break :break
          [:group (v/-text "Found " p) (v/-visit permissions p) (v/-text ", expected " p) (v/-visit expected p)]]})

(defmethod v/-format ::k/invalid-actions [_ _ {:keys [explanation]} p]
  (assoc (v/-format ::m/explain nil (me/with-spell-checking explanation) p) :title "Invalid Actions"))

(defmethod v/-format ::k/input-schema-error [_ _ {:keys [explanation]} p]
  (assoc (v/-format ::m/explain nil explanation p) :title "Input Schema Error"))

(defmethod v/-format ::k/output-schema-error [_ _ {:keys [explanation]} p]
  (assoc (v/-format ::m/explain nil explanation p) :title "Output Schema Error"))

;;
;; Public API
;;

(defn dispatcher
  ([actions] (dispatcher actions (k/-default-options)))
  ([actions options]
   (let [thrower (pretty/thrower (pretty/-printer {:title "Dispatch Error"}))
         catch! #(let [{:keys [type data]} (ex-data %)] (thrower type data))
         dispatcher (try (k/dispatcher actions options)
                         (catch #?(:clj Exception, :cljs js/Error) e (catch! e)))]
     (k/-proxy
      dispatcher
      (fn [env ctx event]
        (try (k/-dispatch dispatcher env ctx event)
             (catch #?(:clj Exception, :cljs js/Error) e (catch! e))))))))
