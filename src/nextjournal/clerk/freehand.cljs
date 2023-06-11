(ns nextjournal.clerk.freehand
  (:require [applied-science.js-interop :as j]
            [clojure.string :as str]
            [nextjournal.clerk.render :as render]
            [nextjournal.clerk.render.hooks :as render.hooks]))

(defn ->svg-p [[x y]] (str x "," y))
(defn ->svg-path [[p & ps]]
  (when (seq ps)
    [:path {:fill "currentColor"
            :stroke "currentColor"
            :stroke-linecap "round"
            :stroke-width 1
            :d (str "M " (->svg-p p) " L " (str/join " " (map ->svg-p ps)) " Z")}]))

(defn store! [s file]
  (render/clerk-eval (list `store! (-> s js->clj
                                       (select-keys [:paths])
                                       (assoc :file file)))))

(defn clear! [file]
  (render/clerk-eval (list `clear! {:file file})))

(defn new-state [{:keys [paths]}] {:path (j/lit [])
                                   :trace (j/lit [])
                                   :paths (or paths [])
                                   :pen-down false})

(defn update-trace [{:as state :keys [trace]} get-stroke ^js e {:keys [bcr-x bcr-y]}]
  (let [new-trace (.concat trace (j/lit [[(- (.-pageX e) bcr-x)
                                          (- (.-pageY e) bcr-y)
                                          (.-pressure e)]]))]
    (-> state
        (assoc :trace new-trace)
        (assoc :path (get-stroke new-trace)))))

(defn add-path [{:as state :keys [path]} {:keys [file]}]
  (-> state
      (update :paths conj (:path state))
      (assoc :trace (j/lit []) :pen-down false :path (j/lit []))
      (doto (store! file))))

(defn pf-svg-drawing [PF {:as opts :keys [file]}]
  (let [!state (render.hooks/use-state (new-state opts))

        pf-opts (j/obj :size 6 :thinning 0.5 :smoothing 0.8 :streamline 0.9)
        get-stroke (fn [pressure-points] (.getStroke PF pressure-points pf-opts))
        ref (render.hooks/use-ref)
        get-bcr (fn [el] (when el
                           (let [bcr (.getBoundingClientRect el)]
                             {:bcr-x (.-left bcr)
                              :bcr-y (.-top bcr)})))
        pointer-move (fn [e]
                       (let [{:keys [pen-down]} @!state]
                         (when pen-down
                           (swap! !state update-trace get-stroke e (get-bcr @ref)))))
        pointer-down (fn [_] (swap! !state assoc :pen-down true))
        pointer-up (fn [_] (swap! !state add-path opts))]
    (let [{:keys [path paths]} @!state]
      [:div.flex
       [:div [:button.border-2.rounded.border-amber-500
              {:on-click (fn [_]
                           (swap! !state assoc :paths [])
                           (clear! file))} "🗑️"]]
       [:svg.text-amber-600.border.border-amber-500.border-4
        {:ref ref :height "50rem" :width "100%"
         :onPointerUp pointer-up :onPointerDown pointer-down :onPointerMove pointer-move}
        [->svg-path path]
        (into [:g] (keep ->svg-path) paths)]])))

(defn svg-drawing [opts _]
  (let [PF (render.hooks/use-dynamic-import "https://cdn.jsdelivr.net/npm/perfect-freehand@1.2.0/dist/esm/index.js")]
    (when PF
      [pf-svg-drawing PF opts])))
