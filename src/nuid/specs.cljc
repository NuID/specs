(ns nuid.specs
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::protocol #{{:id :knizk}})
(s/def ::curve #{{:id :secp256k1}})
(s/def ::normalization-form #{"NFC" "NFKC" "NFD" "NFKD"})

(defmulti keyfn :id)
(defmethod keyfn :sha256 [_] (s/keys :req-un [::salt ::normalization-form]))
(defmethod keyfn :sha512 [_] (s/keys :req-un [::salt ::normalization-form]))
(defmethod keyfn :scrypt [_] (s/keys :req-un [::salt ::n ::r ::p ::normalization-form ::key-length]))
(s/def ::keyfn (s/multi-spec keyfn :id))

(defmulti hashfn :id)
(defmethod hashfn :sha256 [_] (s/keys :req-un [::normalization-form]))
(defmethod hashfn :sha512 [_] (s/keys :req-un [::normalization-form]))
(s/def ::hashfn (s/multi-spec hashfn :id))

(s/def ::credential-specification (s/keys :req-un [::protocol ::curve ::keyfn ::hashfn]))
(s/def ::credential (s/merge ::credential-specification (s/keys :req-un [::pub])))

(defmulti proof* :protocol)
(defmethod proof* {:id :knizk} [_] (s/keys :req-un [::c ::s ::nonce]))
(s/def ::proof* (s/multi-spec proof* :protocol))
(s/def ::proof (s/merge ::credential ::proof*))

(s/def ::ethereum-nil-transaction-id
  #(= % "0x0000000000000000000000000000000000000000000000000000000000000000"))
(s/def ::ethereum-transaction-id
  (s/and string?
         not-empty
         #(= (count %) 66)
         #(= (subs % 0 2) "0x")
         #(not (s/valid? ::ethereum-nil-transaction-id %))))

(s/def ::credential-id (s/or :store.ethereum/credential-id ::ethereum-transaction-id))

(s/def ::has-credential-data (s/and map? #(some (fn [k] (= (namespace k) "credential.data")) (keys %))))
(s/def ::has-credential-id (s/and map? #(some (fn [k] (= (name k) "credential-id")) (keys %))))
(s/def ::initialize-request (s/+ ::credential-id))
(s/def ::initialized-credential (s/merge ::has-credential-id ::has-credential-data (s/keys :req-un [::nonce])))
(s/def ::initialize-response (s/coll-of ::initialized-credential))

(s/def ::verifiable (s/merge ::has-credential-id ::proof))
(s/def ::verify-request (s/coll-of ::verifiable))

(s/def ::secret (s/and string? not-empty))
(s/def ::client-credentials (s/map-of ::credential-id (s/keys :req-un [::secret])))

(s/def ::sqs-submission-success-response
  (s/keys :req-un [::MessageId ::MD5OfMessageBody]))

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

(s/def ::dispatch (s/or :protocol ::credential))
(defn select-keys-spec [m spec]
  (let [multi-spec? (= (keyword (first (s/form spec))) ::s/multi-spec)
        tagged (s/conform ::dispatch m)]
    (if-not (= tagged ::s/invalid)
      (let [dispatch (get (second tagged) (first tagged))
            spec (if multi-spec? (join-kws spec dispatch) spec)]
        (select-keys m (get-keys dispatch spec)))
      (select-keys m (get-keys spec)))))
