;; # ✍️ Clerk Freehand
(ns nextjournal.clerk.freehand
  {:nextjournal.clerk/no-cache true
   :nextjournal.clerk/visibility {:code :hide :result :hide}}
  (:require [nextjournal.clerk :as clerk]
            [clojure.edn :as edn]
            [babashka.fs :as fs]
            [clojure.java.io :as io]))

(def viewer
  {:transform-fn
   (comp clerk/mark-presented
         (clerk/update-val
          (fn [{:as opts :keys [file]}]
            (cond-> opts
              (and file (fs/exists? file))
              (merge (edn/read-string (slurp file)))))))
   :render-fn 'nextjournal.clerk.freehand/svg-drawing})

(defn store! [{:as info :keys [file]}]
  (spit file (pr-str (dissoc info :file))))

(defn clear! [{:keys [file]}]
  (fs/delete-if-exists file))

(defmacro drawing
  ([file] `(drawing {} ~file))
  ([opts file]
   (fs/create-dirs (fs/parent file))
   `(clerk/fragment

     (assoc-in
      ;; hack for hiding results in a fragment
      (clerk/eval-cljs-str (slurp (io/resource "nextjournal/clerk/freehand.cljs")))
      [:nextjournal/viewer :render-fn] '(fn [_ _] [:<>]))

     (clerk/with-viewer viewer ~opts (assoc ~opts :file ~file)))))

{::clerk/visibility {:result :show}}
(drawing {::clerk/widthx :full} "data/drawing.edn")
