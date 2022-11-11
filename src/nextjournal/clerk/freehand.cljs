(ns nextjournal.clerk.freehand
  (:require [nextjournal.clerk.render :as r]
            [applied-science.js-interop :as j]
            [nextjournal.clerk.render.hooks :as render.hooks]))

(defn ->svg-p [[x y]] (str x "," y))
(defn ->svg-path [[p & ps :as points]]
  (when (seq ps)
    [:path {:fill "none" :stroke "currentColor" :stroke-linecap "round" :stroke-width 5
            :d (str "M " (->svg-p p) " L " (clojure.string/join " " (map ->svg-p ps)))}]))

;; with perfect free-hand
#_(r/use-dynamic-import "https://cdn.jsdelivr.net/npm/perfect-freehand@1.2.0/dist/esm/index.js")
#_(def pf-opts (j/obj :size 8 :thinning 0.3 :smoothing 0.5 :streamline 0.9))
#_(defn ->svg-path [pf [p & ps :as points]]
    (when (seq ps)
      (let [[p & ps] (.getStroke pf points pf-opts)]
        [:path {:fill "currentColor" :stroke "currentColor" :stroke-linecap "round"
                :d (str "M " (->svg-p p) " L " (clojure.string/join " " (map ->svg-p ps)) " Z")}])))

(defn svg-drawing [_]
  (let [!state (render.hooks/use-state {:path (j/lit []) :paths [] :pen-down false})
        ref (render.hooks/use-ref)
        get-bcr (fn [el] (when el
                           (let [bcr (.getBoundingClientRect el)]
                             {:bcr-x (.-left bcr)
                              :bcr-y (.-top bcr)})))
        pointer-move (fn [e]
                       (let [{:keys [pen-down]} @!state]
                         (when pen-down
                           (let [{:keys [bcr-x bcr-y]} (get-bcr @ref)]
                             (swap! !state update :path
                                    #(.concat %1 (j/lit [[(- (.-pageX e) bcr-x)
                                                          (- (.-pageY e) bcr-y)
                                                          (.-pressure e)]])))))))
        pointer-down (fn [_] (js/console.log "ponter-down")
                       (swap! !state assoc :pen-down true))
        pointer-up (fn [_] (swap! !state (fn [s] (-> s
                                                     (assoc :pen-down false)
                                                     (update :paths conj (:path s))
                                                     (assoc :path (j/lit []))))))]
    (let [{:keys [path paths]} @!state]
      [:svg.text-amber-600.border.border-amber-500.border-4
       {:ref ref :height "50rem" :width "100%"
        :onPointerUp pointer-up :onPointerDown pointer-down :onPointerMove pointer-move}
       [->svg-path path]
       (into [:g] (keep ->svg-path) paths)])))
