;; # ✍️ Clerk Freehand
(ns nextjournal.clerk.freehand
  {:nextjournal.clerk/no-cache true
   :nextjournal.clerk/visibility {:code :hide :result :hide}}
  (:require [nextjournal.clerk :as clerk]
            [babashka.fs :as fs]
            [clojure.java.io :as io]))

(def viewer
  {:transform-fn
   (comp clerk/mark-presented
         (clerk/update-val
          (fn [{:as opts :keys [path]}]
            (cond-> opts
              (and path (fs/exists? path))
              (assoc :svg (slurp path))))))
   :render-fn 'nextjournal.clerk.freehand/svg-drawing})

(defn drawing
  ([path] (drawing path {}))
  ([path opts]
   (clerk/eval-cljs-str (slurp (io/resource "nextjournal/clerk/freehand.cljs")))
   (clerk/with-viewer viewer {::clerk/width :full}
                 (assoc opts :path path))))

{::clerk/visibility {:result :show}}
(drawing "data/first_drawing.svg")
