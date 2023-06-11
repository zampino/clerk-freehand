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

(clerk/eval-cljs-str (slurp (io/resource "nextjournal/clerk/freehand.cljs")))

(defn store! [{:as info :keys [file]}]
  (spit file (pr-str (dissoc info :file))))

(defn clear! [{:keys [file]}]
  (fs/delete-if-exists file))

(defn drawing
  ([file] (drawing file {}))
  ([file opts]
   (fs/create-dirs (fs/parent file))
   (clerk/with-viewer viewer {::clerk/width :full}
     (assoc opts :file file))))

{::clerk/visibility {:result :show}}
(drawing "data/drawing-2.edn")
