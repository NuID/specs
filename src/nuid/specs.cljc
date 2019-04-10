(ns nuid.specs
  (:require [clojure.spec.alpha :as s]))

(def unqualify (comp keyword name))

(defn parse-keys-form
  "Extracts the keys (`:req`, `:opt`, `:req-un`, and `:opt-un`) from
  an `s/keys` form and `concat`s them into a flat list.
  `s/select` obviates this function."
  [form]
  (let [{:keys [req req-un opt opt-un]} (apply hash-map (rest form))]
    (concat req (map unqualify req-un) opt (map unqualify opt-un))))

(defn join-kws
  "Joins a variable length list of keywords into a single keyword,
  separated by hyphens. The new keyword will have the same namespaces as
  the first keyword in the list. This is used to exploit naming conventions
  while descending into a `s/multi-spec` to extract relevant keys.
  `s/select` obviates this function."
  [& kws]
  (when (first kws)
    (keyword
     (namespace (first kws))
     (clojure.string/join
      "-"
      (map name (filter some? kws))))))

(defn get-keys
  "Tries to descend into nested s/merge'd specs to extract keys.
  `dispatch` may be used to exploit a naming convention within s/multi-specs.
  `s/select`'s obviates this function."
  ([spec] (get-keys nil spec))
  ([dispatch spec]
   (let [form (if (keyword? spec) (s/form spec) spec)]
     (when (seqable? form)
       (->> (condp = (keyword (first form))
              ::s/multi-spec (get-keys dispatch (join-kws (keyword (second form)) dispatch))
              ::s/merge (map (partial get-keys dispatch) (rest form))
              ::s/keys (parse-keys-form form))
            flatten
            (filter some?))))))
