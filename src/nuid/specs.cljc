(ns nuid.specs
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::protocol #{:schnorrsnizk})
(s/def ::curve #{:ed25519 :secp256k1})

(s/def ::versioned
  (s/keys :req-un [::protocol ::curve ::keyfn ::hashfn]))

(s/def ::zk-parameters-schnorrsnizk
  (s/merge ::versioned (s/keys :req-un [::r1 ::r2 ::c] :opt-un [::pub])))
(defmulti zk-parameters :protocol)
(defmethod zk-parameters :schnorrsnizk [_] ::zk-parameters-schnorrsnizk)
(s/def ::zk-parameters (s/multi-spec zk-parameters :protocol))

(s/def ::zk-proof-schnorrsnizk
  (s/merge ::versioned (s/keys :req-un [::A ::C ::s1 ::s2])))
(defmulti zk-proof :protocol)
(defmethod zk-proof :schnorrsnizk [_] ::zk-proof-schnorrsnizk)
(s/def ::zk-proof (s/multi-spec zk-proof :protocol))

(s/def ::register-request-body
  (s/merge ::zk-proof ::zk-parameters (s/keys :req-un [::b])))

(s/def ::ethereum-zero-hash
  #(= % "0x0000000000000000000000000000000000000000000000000000000000000000"))

(s/def ::ethereum-transaction-id
  (s/and string?
         not-empty
         #(= (count %) 66)
         #(= (subs % 0 2) "0x")
         #(not (s/valid? ::ethereum-zero-hash %))))

(s/def ::transaction-id
  (s/and string?
         not-empty
         #(= (count %) 66)
         #(= (subs % 0 2) "0x")))

(s/def ::initialize-request-body
  (s/+ ::transaction-id))

(s/def ::initialize-response-body
  (s/map-of ::transaction-id (s/merge ::zk-parameters (s/keys :req-un [::b]))))

(s/def ::verify-request-body
  (s/map-of ::transaction-id ::zk-proof))

(s/def ::secret
  (s/and string? not-empty))

(s/def ::client-credentials
  (s/map-of ::transaction-id (s/keys :req-un [::secret])))

(def unqualify (comp keyword name))

(defn parse-keys-form [form]
  (let [{:keys [req req-un opt opt-un]} (apply hash-map (rest form))]
    (concat req (map unqualify req-un) opt (map unqualify opt-un))))

(defn join-kws [& kws]
  (when (first kws)
    (keyword
     (namespace (first kws))
     (clojure.string/join
      "-"
      (map name (filter some? kws))))))

(defn get-keys
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

(s/def ::dispatch (s/or :protocol ::versioned))
(defn select-keys-spec [m spec]
  (let [multi-spec? (= (keyword (first (s/form spec))) ::s/multi-spec)
        tagged (s/conform ::dispatch m)]
    (if-not (= tagged ::s/invalid)
      (let [dispatch (get (second tagged) (first tagged))
            spec (if multi-spec? (join-kws spec dispatch) spec)]
        (select-keys m (get-keys dispatch spec)))
      (select-keys m (get-keys spec)))))
