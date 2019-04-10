(ns nuid.specs.credential
  (:require [clojure.spec.alpha :as s]))

(s/def ::protocol #{{:id :knizk}})
(s/def ::curve #{{:id :secp256k1}})
(s/def ::normalization-form #{"NFC" "NFKC" "NFD" "NFKD"})

(s/def ::normalization-form? (s/keys :req-un [::normalization-form]))
(s/def ::salt? (s/keys :req-un [::salt]))

(s/def ::keyfn* (s/merge ::salt? ::normalization-form?))
(s/def ::keyfn-sha256 ::keyfn*)
(s/def ::keyfn-sha512 ::keyfn*)
(s/def ::keyfn-scrypt (s/merge ::keyfn* (s/keys :req-un [::n ::r ::p ::key-length])))
(defmulti keyfn :id)
(defmethod keyfn :sha256 [_] ::keyfn-sha256)
(defmethod keyfn :sha512 [_] ::keyfn-sha512)
(defmethod keyfn :scrypt [_] ::keyfn-scrypt)
(s/def ::keyfn (s/multi-spec keyfn :id))

(s/def ::hashfn-sha256 ::normalization-form?)
(s/def ::hashfn-sha512 ::normalization-form?)
(defmulti hashfn :id)
(defmethod hashfn :sha256 [_] ::hashfn-sha256)
(defmethod hashfn :sha512 [_] ::hashfn-sha512)
(s/def ::hashfn (s/multi-spec hashfn :id))

(s/def ::parameters (s/keys :req-un [::protocol ::curve ::keyfn ::hashfn ::pub]))

(defmulti proof* :protocol)
(defmethod proof* {:id :knizk} [_] (s/keys :req-un [::c ::s ::nonce]))
(s/def ::proof* (s/multi-spec proof* :protocol))
(s/def ::proof (s/merge ::parameters ::proof*))

(s/def ::id (s/or :store.ethereum/id :ethereum/transaction-id))

(s/def ::data? (s/and map? #(some (fn [k] (= (namespace k) "credential.data")) (keys %))))
(s/def ::id? (s/and map? #(some (fn [k] (= (name k) "id")) (keys %))))
(s/def ::initialized (s/merge ::id? ::data? (s/keys :req-un [::nonce])))

(s/def ::initialize-request (s/+ ::id))
(s/def ::initialize-response (s/coll-of ::initialized))

(s/def ::verifiable (s/merge ::id? ::proof))
(s/def ::verify-request (s/coll-of ::verifiable))
